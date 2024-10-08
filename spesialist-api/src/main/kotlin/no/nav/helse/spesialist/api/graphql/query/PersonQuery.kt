package no.nav.helse.spesialist.api.graphql.query

import graphql.GraphQLContext
import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.bootstrap.Environment
import no.nav.helse.spesialist.api.Avviksvurderinghenter
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.StansAutomatiskBehandlinghåndterer
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.auditLogTeller
import no.nav.helse.spesialist.api.egenAnsatt.EgenAnsattApiDao
import no.nav.helse.spesialist.api.graphql.ContextValues.SAKSBEHANDLER
import no.nav.helse.spesialist.api.graphql.ContextValues.TILGANGER
import no.nav.helse.spesialist.api.graphql.query.Inputvalidering.UgyldigInput
import no.nav.helse.spesialist.api.graphql.schema.Person
import no.nav.helse.spesialist.api.graphql.schema.Reservasjon
import no.nav.helse.spesialist.api.notat.NotatDao
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.oppgave.Oppgavehåndterer
import no.nav.helse.spesialist.api.overstyring.OverstyringApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.person.PersonApiDao
import no.nav.helse.spesialist.api.påvent.PåVentApiDao
import no.nav.helse.spesialist.api.reservasjon.ReservasjonClient
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.snapshot.SnapshotService
import no.nav.helse.spesialist.api.tildeling.TildelingApiDao
import no.nav.helse.spesialist.api.totrinnsvurdering.TotrinnsvurderingApiDao
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import no.nav.helse.spesialist.api.vergemål.VergemålApiDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private sealed class Inputvalidering {
    class Ok(val fødselsnummer: String) : Inputvalidering()

    sealed class UgyldigInput(val graphqlError: GraphQLError) : Inputvalidering() {
        class UkjentFødselsnummer(val fødselsnummer: String, graphqlError: GraphQLError) : UgyldigInput(graphqlError)

        class UkjentAktørId(val aktørId: String, graphqlError: GraphQLError) : UgyldigInput(graphqlError)

        class ParametreMangler(graphqlError: GraphQLError) : UgyldigInput(graphqlError)

        class UgyldigAktørId(graphqlError: GraphQLError) : UgyldigInput(graphqlError)

        class HarFlereFødselsnumre(val aktørId: String, graphqlError: GraphQLError) : UgyldigInput(graphqlError)
    }
}

class PersonQuery(
    personApiDao: PersonApiDao,
    egenAnsattApiDao: EgenAnsattApiDao,
    private val tildelingApiDao: TildelingApiDao,
    private val arbeidsgiverApiDao: ArbeidsgiverApiDao,
    private val overstyringApiDao: OverstyringApiDao,
    private val risikovurderingApiDao: RisikovurderingApiDao,
    private val varselRepository: ApiVarselRepository,
    private val oppgaveApiDao: OppgaveApiDao,
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val notatDao: NotatDao,
    private val totrinnsvurderingApiDao: TotrinnsvurderingApiDao,
    private val påVentApiDao: PåVentApiDao,
    private val vergemålApiDao: VergemålApiDao,
    private val snapshotService: SnapshotService,
    private val reservasjonClient: ReservasjonClient,
    private val oppgavehåndterer: Oppgavehåndterer,
    private val saksbehandlerhåndterer: Saksbehandlerhåndterer,
    private val avviksvurderinghenter: Avviksvurderinghenter,
    private val stansAutomatiskBehandlinghåndterer: StansAutomatiskBehandlinghåndterer,
) : AbstractPersonQuery(personApiDao, egenAnsattApiDao) {
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
    private val auditLog = LoggerFactory.getLogger("auditLogger")
    private val env = Environment()

    suspend fun person(
        fnr: String? = null,
        aktorId: String? = null,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Person?> {
        val fødselsnummer =
            when (val validering = validerInput(fnr, aktorId)) {
                is Inputvalidering.Ok -> validering.fødselsnummer
                is UgyldigInput -> {
                    validering.auditlogg(env)
                    return validering.graphqlError.tilGraphqlResult()
                }
            }
        sikkerLogg.info("Personoppslag på fnr=$fødselsnummer")

        if (!personApiDao.spesialistHarPersonKlarForVisningISpeil(fødselsnummer)) {
            auditLog(env.graphQlContext, fødselsnummer, false, null)
            return DataFetcherResult.newResult<Person?>()
                .error(getPersonNotReadyError(fødselsnummer))
                .extensions(mapOf("persondata_hentes" to true))
                .build()
        }

        if (isForbidden(fødselsnummer, env)) {
            auditLog(env.graphQlContext, fødselsnummer, false, null)
            return lagTomtResultat(getForbiddenError(fødselsnummer))
        }

        val reservasjon = finnReservasjonsstatus(fødselsnummer)
        val unntattFraAutomatiskGodkjenning = unntattFraAutomatiskGodkjenning(fødselsnummer)

        val snapshot =
            try {
                snapshotService.hentSnapshot(fødselsnummer)
            } catch (e: Exception) {
                sikkerLogg.error("feilet under henting av snapshot for {}", keyValue("fnr", fødselsnummer), e)
                auditLog(env.graphQlContext, fødselsnummer, null, getSnapshotValidationError().message)
                return lagTomtResultat(getSnapshotValidationError())
            }

        val person =
            snapshot?.let { (personinfo, personSnapshot) ->
                Person(
                    snapshot = personSnapshot,
                    personinfo =
                        personinfo.copy(
                            reservasjon = reservasjon.await(),
                            unntattFraAutomatisering = unntattFraAutomatiskGodkjenning,
                            fullmakt = vergemålApiDao.harFullmakt(fødselsnummer),
                        ),
                    personApiDao = personApiDao,
                    tildelingApiDao = tildelingApiDao,
                    arbeidsgiverApiDao = arbeidsgiverApiDao,
                    overstyringApiDao = overstyringApiDao,
                    risikovurderingApiDao = risikovurderingApiDao,
                    varselRepository = varselRepository,
                    oppgaveApiDao = oppgaveApiDao,
                    periodehistorikkDao = periodehistorikkDao,
                    notatDao = notatDao,
                    totrinnsvurderingApiDao = totrinnsvurderingApiDao,
                    påVentApiDao = påVentApiDao,
                    avviksvurderinghenter = avviksvurderinghenter,
                    tilganger = env.graphQlContext.get(TILGANGER),
                    oppgavehåndterer = oppgavehåndterer,
                    saksbehandlerhåndterer = saksbehandlerhåndterer,
                )
            }

        return if (person == null) {
            auditLog(env.graphQlContext, fødselsnummer, true, getNotFoundError(fødselsnummer).message)
            lagTomtResultat(getNotFoundError(fødselsnummer))
        } else {
            auditLog(env.graphQlContext, fødselsnummer, true, null)
            DataFetcherResult.newResult<Person?>().data(person).build()
        }
    }

    private fun Set<String>.harFlereFødselsnumre() = this.size > 1

    private fun Set<String>.harIngenFødselsnumre() = this.isEmpty()

    private fun validerInput(
        fødselsnummer: String?,
        aktørId: String?,
    ): Inputvalidering {
        if (fødselsnummer != null) {
            if (personApiDao.finnesPersonMedFødselsnummer(fødselsnummer)) return Inputvalidering.Ok(fødselsnummer)
            return UgyldigInput.UkjentFødselsnummer(fødselsnummer, getNotFoundError(fødselsnummer))
        }
        if (aktørId == null) return UgyldigInput.ParametreMangler(getBadRequestError("Requesten mangler både fødselsnummer og aktorId"))
        if (aktørId.length != 13) {
            return UgyldigInput.UgyldigAktørId(
                getBadRequestError("Feil lengde på parameter aktorId: ${aktørId.length}"),
            )
        }

        val fødselsnumre = personApiDao.finnFødselsnumre(aktørId.toLong()).toSet()

        if (fødselsnumre.harIngenFødselsnumre()) return UgyldigInput.UkjentAktørId(aktørId, getNotFoundError(aktørId))
        if (fødselsnumre.harFlereFødselsnumre()) return UgyldigInput.HarFlereFødselsnumre(aktørId, getFlereFødselsnumreError(fødselsnumre))

        return Inputvalidering.Ok(fødselsnumre.single())
    }

    private fun lagTomtResultat(error: GraphQLError): DataFetcherResult<Person?> =
        DataFetcherResult.newResult<Person?>().error(error).build()

    private fun GraphQLError.tilGraphqlResult(): DataFetcherResult<Person?> = DataFetcherResult.newResult<Person?>().error(this).build()

    private fun unntattFraAutomatiskGodkjenning(fødselsnummer: String) =
        stansAutomatiskBehandlinghåndterer.unntattFraAutomatiskGodkjenning(fødselsnummer)

    private fun finnReservasjonsstatus(fødselsnummer: String) =
        if (env.erDev) {
            CompletableDeferred<Reservasjon?>().also { it.complete(null) }
        } else {
            CoroutineScope(Dispatchers.IO).async {
                reservasjonClient.hentReservasjonsstatus(fødselsnummer)
            }
        }

    private fun loggNotFoundForAktørId(
        aktorId: String,
        env: DataFetchingEnvironment,
    ) {
        sikkerLogg.info("Svarer not found for parametere aktorId=$aktorId.")
        auditLog(env.graphQlContext, aktorId, null, getNotFoundError(aktorId).message)
    }

    private fun loggNotFoundForFødselsnummer(
        fnr: String,
        env: DataFetchingEnvironment,
    ) {
        sikkerLogg.info("Svarer not found for parametere fnr=$fnr.")
        auditLog(env.graphQlContext, fnr, null, getNotFoundError(fnr).message)
    }

    private fun getFlereFødselsnumreError(fødselsnumre: Set<String>): GraphQLError =
        GraphqlErrorException
            .newErrorException()
            .message("Mer enn ett fødselsnummer for personen")
            .extensions(
                mapOf(
                    "code" to 500,
                    "feilkode" to "HarFlereFodselsnumre",
                    "fodselsnumre" to fødselsnumre,
                ),
            ).build()

    private fun getSnapshotValidationError(): GraphQLError =
        GraphqlErrorException
            .newErrorException()
            .message(
                "Lagret snapshot stemmer ikke overens med forventet format. Dette kommer som regel av at noen har gjort endringer på formatet men glemt å bumpe versjonsnummeret.",
            ).extensions(mapOf("code" to 501, "field" to "person"))
            .build()

    private fun getBadRequestError(melding: String): GraphQLError =
        GraphqlErrorException
            .newErrorException()
            .message(melding)
            .extensions(mapOf("code" to 400))
            .build()

    private fun auditLog(
        graphQLContext: GraphQLContext,
        personId: String,
        harTilgang: Boolean?,
        fantIkkePersonErrorMsg: String?,
    ) {
        val saksbehandlerIdent = graphQLContext.get<SaksbehandlerFraApi>(SAKSBEHANDLER).ident
        auditLogTeller.inc()

        if (harTilgang == false) {
            auditLog.warn(
                "end=${System.currentTimeMillis()} suid=$saksbehandlerIdent duid=$personId operation=PersonQuery flexString1=Deny",
            )
        } else if (fantIkkePersonErrorMsg != null) {
            auditLog.warn(
                "end=${System.currentTimeMillis()} suid=$saksbehandlerIdent duid=$personId operation=PersonQuery msg=$fantIkkePersonErrorMsg",
            )
        } else {
            auditLog.info("end=${System.currentTimeMillis()} suid=$saksbehandlerIdent duid=$personId operation=PersonQuery")
        }
        sikkerLogg.debug(
            "audit-logget, operationName: PersonQuery, harTilgang: $harTilgang, fantIkkePersonErrorMsg: $fantIkkePersonErrorMsg",
        )
    }

    private fun UgyldigInput.auditlogg(env: DataFetchingEnvironment) {
        when (this) {
            is UgyldigInput.UkjentFødselsnummer -> loggNotFoundForFødselsnummer(this.fødselsnummer, env)
            is UgyldigInput.UkjentAktørId -> loggNotFoundForAktørId(aktørId, env)
            is UgyldigInput.HarFlereFødselsnumre -> auditLog(env.graphQlContext, aktørId, null, graphqlError.message)
            is UgyldigInput.ParametreMangler -> {}
            is UgyldigInput.UgyldigAktørId -> {}
        }
    }
}
