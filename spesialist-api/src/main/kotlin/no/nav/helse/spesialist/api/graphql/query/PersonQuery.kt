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
import no.nav.helse.spesialist.api.Avviksvurderinghenter
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.StansAutomatiskBehandlinghåndterer
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.auditLogTeller
import no.nav.helse.spesialist.api.egenAnsatt.EgenAnsattApiDao
import no.nav.helse.spesialist.api.erDev
import no.nav.helse.spesialist.api.graphql.ContextValues
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
import no.nav.helse.spesialist.api.snapshot.SnapshotService
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import no.nav.helse.spesialist.api.totrinnsvurdering.TotrinnsvurderingApiDao
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PersonQuery(
    personApiDao: PersonApiDao,
    egenAnsattApiDao: EgenAnsattApiDao,
    private val tildelingDao: TildelingDao,
    private val arbeidsgiverApiDao: ArbeidsgiverApiDao,
    private val overstyringApiDao: OverstyringApiDao,
    private val risikovurderingApiDao: RisikovurderingApiDao,
    private val varselRepository: ApiVarselRepository,
    private val oppgaveApiDao: OppgaveApiDao,
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val notatDao: NotatDao,
    private val totrinnsvurderingApiDao: TotrinnsvurderingApiDao,
    private val påVentApiDao: PåVentApiDao,
    private val snapshotService: SnapshotService,
    private val reservasjonClient: ReservasjonClient,
    private val oppgavehåndterer: Oppgavehåndterer,
    private val saksbehandlerhåndterer: Saksbehandlerhåndterer,
    private val avviksvurderinghenter: Avviksvurderinghenter,
    private val stansAutomatiskBehandlinghåndterer: StansAutomatiskBehandlinghåndterer,
) : AbstractPersonQuery(personApiDao, egenAnsattApiDao) {
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
    private val auditLog = LoggerFactory.getLogger("auditLogger")

    suspend fun person(
        fnr: String? = null,
        aktorId: String? = null,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<Person?> {
        if (fnr == null) {
            if (aktorId == null) {
                return DataFetcherResult.newResult<Person?>()
                    .error(getBadRequestError("Requesten mangler både fødselsnummer og aktørId")).build()
            }
            if (aktorId.length != 13) {
                return DataFetcherResult.newResult<Person?>()
                    .error(getBadRequestError("Feil lengde på parameter aktorId: ${aktorId.length}")).build()
            }
        }

        val fødselsnummer =
            if (fnr != null && personApiDao.finnesPersonMedFødselsnummer(fnr)) {
                fnr
            } else {
                aktorId?.let {
                    try {
                        personApiDao.finnFødselsnummer(it.toLong())
                    } catch (e: Exception) {
                        val fødselsnumre = personApiDao.finnFødselsnumre(aktorId.toLong()).toSet()
                        auditLog(env.graphQlContext, aktorId, null, getFlereFødselsnumreError(fødselsnumre).message)
                        return DataFetcherResult.newResult<Person?>().error(getFlereFødselsnumreError(fødselsnumre))
                            .build()
                    }
                }
            }
        if (fødselsnummer == null || (!erDev() && !personApiDao.spesialistHarPersonKlarForVisningISpeil(fødselsnummer))) {
            sikkerLogg.info("Svarer not found for parametere fnr=$fnr, aktorId=$aktorId.")
            auditLog(env.graphQlContext, fnr ?: aktorId!!, null, getNotFoundError(fnr).message)
            return DataFetcherResult.newResult<Person?>().error(getNotFoundError(fnr)).build()
        }

        if (isForbidden(fødselsnummer, env)) {
            auditLog(env.graphQlContext, fødselsnummer, false, null)
            return DataFetcherResult.newResult<Person?>().error(getForbiddenError(fødselsnummer)).build()
        }

        val reservasjon = finnReservasjonsstatus(fødselsnummer)
        val unntattFraAutomatiskGodkjenning = unntattFraAutomatiskGodkjenning(fødselsnummer)

        val snapshot =
            try {
                snapshotService.hentSnapshot(fødselsnummer)
            } catch (e: Exception) {
                sikkerLogg.error("feilet under henting av snapshot for {}", keyValue("fnr", fødselsnummer), e)
                auditLog(env.graphQlContext, fødselsnummer, null, getSnapshotValidationError().message)
                return DataFetcherResult.newResult<Person?>().error(getSnapshotValidationError()).build()
            }

        val person =
            snapshot?.let { (personinfo, personSnapshot) ->
                Person(
                    snapshot = personSnapshot,
                    personinfo =
                        personinfo.copy(
                            reservasjon = reservasjon.await(),
                            unntattFraAutomatisering = unntattFraAutomatiskGodkjenning,
                        ),
                    personApiDao = personApiDao,
                    tildelingDao = tildelingDao,
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
                    tilganger = env.graphQlContext.get("tilganger"),
                    oppgavehåndterer = oppgavehåndterer,
                    saksbehandlerhåndterer = saksbehandlerhåndterer,
                )
            }

        return if (person == null) {
            auditLog(env.graphQlContext, fødselsnummer, true, getNotFoundError(fødselsnummer).message)
            DataFetcherResult.newResult<Person?>().error(getNotFoundError(fødselsnummer)).build()
        } else {
            auditLog(env.graphQlContext, fødselsnummer, true, null)
            DataFetcherResult.newResult<Person?>().data(person).build()
        }
    }

    private fun unntattFraAutomatiskGodkjenning(fødselsnummer: String) =
        stansAutomatiskBehandlinghåndterer.unntattFraAutomatiskGodkjenning(fødselsnummer)

    private fun finnReservasjonsstatus(fødselsnummer: String) =
        if (erDev()) {
            CompletableDeferred<Reservasjon?>().also { it.complete(null) }
        } else {
            CoroutineScope(Dispatchers.IO).async {
                reservasjonClient.hentReservasjonsstatus(fødselsnummer)
            }
        }

    private fun getFlereFødselsnumreError(fødselsnumre: Set<String>): GraphQLError =
        GraphqlErrorException.newErrorException()
            .message("Mer enn ett fødselsnummer for personen")
            .extensions(
                mapOf(
                    "code" to 500,
                    "feilkode" to "HarFlereFodselsnumre",
                    "fodselsnumre" to fødselsnumre,
                ),
            ).build()

    private fun getSnapshotValidationError(): GraphQLError =
        GraphqlErrorException.newErrorException()
            .message(
                "Lagret snapshot stemmer ikke overens med forventet format. Dette kommer som regel av at noen har gjort endringer på formatet men glemt å bumpe versjonsnummeret.",
            )
            .extensions(mapOf("code" to 501, "field" to "person"))
            .build()

    private fun getBadRequestError(melding: String): GraphQLError =
        GraphqlErrorException.newErrorException()
            .message(melding)
            .extensions(mapOf("code" to 400))
            .build()

    private fun auditLog(
        graphQLContext: GraphQLContext,
        personId: String,
        harTilgang: Boolean?,
        fantIkkePersonErrorMsg: String?,
    ) {
        val saksbehandlerIdent = graphQLContext.get<String>(ContextValues.SAKSBEHANDLER_IDENT.key)
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
}
