package no.nav.helse.spesialist.api.graphql.query

import graphql.GraphQLError
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.db.AnnulleringRepository
import no.nav.helse.db.SessionContext
import no.nav.helse.db.SessionFactory
import no.nav.helse.db.StansAutomatiskBehandlingSaksbehandlerDao
import no.nav.helse.db.VedtakBegrunnelseDao
import no.nav.helse.db.api.ArbeidsgiverApiDao
import no.nav.helse.db.api.NotatApiDao
import no.nav.helse.db.api.OppgaveApiDao
import no.nav.helse.db.api.OverstyringApiDao
import no.nav.helse.db.api.PeriodehistorikkApiDao
import no.nav.helse.db.api.PersonApiDao
import no.nav.helse.db.api.PåVentApiDao
import no.nav.helse.db.api.RisikovurderingApiDao
import no.nav.helse.db.api.TildelingApiDao
import no.nav.helse.db.api.VarselApiRepository
import no.nav.helse.db.api.VergemålApiDao
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.spesialist.api.Personhåndterer
import no.nav.helse.spesialist.api.StansAutomatiskBehandlinghåndterer
import no.nav.helse.spesialist.api.auditLogTeller
import no.nav.helse.spesialist.api.graphql.ContextValues
import no.nav.helse.spesialist.api.graphql.byggFeilrespons
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.forbiddenError
import no.nav.helse.spesialist.api.graphql.graphqlErrorException
import no.nav.helse.spesialist.api.graphql.notFoundError
import no.nav.helse.spesialist.api.graphql.personNotReadyError
import no.nav.helse.spesialist.api.graphql.resolvers.ApiPersonResolver
import no.nav.helse.spesialist.api.graphql.schema.ApiPerson
import no.nav.helse.spesialist.api.snapshot.SnapshotService
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.SaksbehandlerRepository
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import org.slf4j.LoggerFactory

sealed interface FetchPersonResult {
    class Ok(
        val person: ApiPerson,
    ) : FetchPersonResult

    sealed interface Feil : FetchPersonResult {
        class IkkeKlarTilVisning(
            val aktørId: String,
        ) : Feil

        data object ManglerTilgang : Feil

        data object IkkeFunnet : Feil

        data object KlarteIkkeHente : Feil
    }
}

class PersonQueryHandler(
    private val personApiDao: PersonApiDao,
    private val vergemålApiDao: VergemålApiDao,
    private val tildelingApiDao: TildelingApiDao,
    private val arbeidsgiverApiDao: ArbeidsgiverApiDao,
    private val overstyringApiDao: OverstyringApiDao,
    private val risikovurderingApiDao: RisikovurderingApiDao,
    private val varselRepository: VarselApiRepository,
    private val oppgaveApiDao: OppgaveApiDao,
    private val periodehistorikkApiDao: PeriodehistorikkApiDao,
    private val notatDao: NotatApiDao,
    private val påVentApiDao: PåVentApiDao,
    private val apiOppgaveService: ApiOppgaveService,
    private val saksbehandlerMediator: SaksbehandlerMediator,
    private val stansAutomatiskBehandlinghåndterer: StansAutomatiskBehandlinghåndterer,
    private val personhåndterer: Personhåndterer,
    private val snapshotService: SnapshotService,
    private val sessionFactory: SessionFactory,
    private val vedtakBegrunnelseDao: VedtakBegrunnelseDao,
    private val stansAutomatiskBehandlingSaksbehandlerDao: StansAutomatiskBehandlingSaksbehandlerDao,
    private val annulleringRepository: AnnulleringRepository,
    private val saksbehandlerRepository: SaksbehandlerRepository,
) : PersonQuerySchema {
    private val auditLog = LoggerFactory.getLogger("auditLogger")

    override suspend fun person(
        personPseudoId: String,
        env: DataFetchingEnvironment,
    ): DataFetcherResult<ApiPerson?> =
        sessionFactory.transactionalSessionScope { transaction ->
            hentPerson(
                personPseudoId =
                    runCatching {
                        PersonPseudoId.fraString(
                            personPseudoId,
                        )
                    }.getOrElse { return@transactionalSessionScope byggFeilrespons(notFoundError(personPseudoId)) },
                transaction = transaction,
                saksbehandler = env.graphQlContext.get<Saksbehandler>(ContextValues.SAKSBEHANDLER),
                tilgangsgrupper = env.graphQlContext.get<Set<Tilgangsgruppe>>(ContextValues.TILGANGSGRUPPER),
            )
        }

    private fun hentPerson(
        personPseudoId: PersonPseudoId,
        transaction: SessionContext,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
    ): DataFetcherResult<ApiPerson?> {
        val fødselsnummer =
            (
                transaction.personPseudoIdDao.hentIdentitetsnummer(personPseudoId)
                    ?: run {
                        loggNotFoundForPersonPseudoId(personPseudoId.value.toString(), saksbehandler)
                        return byggFeilrespons(notFoundError(personPseudoId.value.toString()))
                    }
            ).value

        loggInfo(
            "Personoppslag på person",
            "fødselsnummer: $fødselsnummer",
        )

        val result =
            utfoerHenting(
                transaction,
                fødselsnummer,
                tilgangsgrupper,
                saksbehandler,
            )

        return when (result) {
            is FetchPersonResult.Feil -> {
                result.auditlogg(saksbehandler, fødselsnummer)
                result.tilGraphqlError(fødselsnummer)
            }

            is FetchPersonResult.Ok -> {
                auditLog(saksbehandler, fødselsnummer, true, null)
                byggRespons(result.person)
            }
        }
    }

    private fun FetchPersonResult.Feil.auditlogg(
        saksbehandler: Saksbehandler,
        fødselsnummer: String,
    ) {
        when (this) {
            is FetchPersonResult.Feil.IkkeFunnet -> {
                auditLog(saksbehandler, `fødselsnummer`, true, notFoundError(`fødselsnummer`).message)
            }

            is FetchPersonResult.Feil.IkkeKlarTilVisning -> {
                auditLog(saksbehandler, `fødselsnummer`, false, null)
            }

            is FetchPersonResult.Feil.ManglerTilgang -> {
                auditLog(saksbehandler, `fødselsnummer`, false, null)
            }

            is FetchPersonResult.Feil.KlarteIkkeHente -> {
                auditLog(saksbehandler, `fødselsnummer`, null, getSnapshotFetchError().message)
            }
        }
    }

    private fun FetchPersonResult.Feil.tilGraphqlError(fødselsnummer: String): DataFetcherResult<ApiPerson?> {
        val graphqlError =
            when (this) {
                is FetchPersonResult.Feil.IkkeFunnet -> notFoundError(fødselsnummer)
                is FetchPersonResult.Feil.IkkeKlarTilVisning -> personNotReadyError(fødselsnummer, aktørId)
                is FetchPersonResult.Feil.ManglerTilgang -> forbiddenError(fødselsnummer)
                is FetchPersonResult.Feil.KlarteIkkeHente -> getSnapshotFetchError()
            }

        return byggFeilrespons(graphqlError)
    }

    private fun loggNotFoundForPersonPseudoId(
        personPseudoId: String,
        saksbehandler: Saksbehandler,
    ) {
        loggInfo("Fant ikke person basert på personPseudoId: $personPseudoId")
        auditLog(saksbehandler, personPseudoId, null, notFoundError(personPseudoId).message)
    }

    private fun getSnapshotFetchError(): GraphQLError = graphqlErrorException(501, "Feil ved henting av snapshot for person", "field" to "person")

    private fun auditLog(
        saksbehandler: Saksbehandler,
        personId: String,
        harTilgang: Boolean?,
        fantIkkePersonErrorMsg: String?,
    ) {
        val saksbehandlerIdent = saksbehandler.ident.value
        auditLogTeller.increment()
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
        sikkerlogg.debug(
            "audit-logget, operationName: PersonQuery, harTilgang: $harTilgang, fantIkkePersonErrorMsg: $fantIkkePersonErrorMsg",
        )
    }

    private fun utfoerHenting(
        transaction: SessionContext,
        fødselsnummer: String,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        saksbehandler: Saksbehandler,
    ): FetchPersonResult {
        val personEntity =
            transaction.personRepository.finn(Identitetsnummer.fraString(`fødselsnummer`))
                ?: return FetchPersonResult.Feil.IkkeFunnet
        if (!personEntity.harDataNødvendigForVisning()) {
            if (!transaction.personKlargjoresDao.klargjøringPågår(`fødselsnummer`)) {
                personhåndterer.klargjørPersonForVisning(`fødselsnummer`)
                transaction.personKlargjoresDao.personKlargjøres(`fødselsnummer`)
            }

            return FetchPersonResult.Feil.IkkeKlarTilVisning(personEntity.aktørId)
        }
        if (!personEntity.kanSeesAvSaksbehandlerMedGrupper(tilgangsgrupper)) {
            return FetchPersonResult.Feil.ManglerTilgang
        }

        // Best effort for å finne ut om saksbehandler har tilgang til oppgaven som gjelder
        // Litt vanskelig å få pent så lenge vi har dynamisk resolving av resten, og tilsynelatende "mange" oppgaver
        val harTilgangTilOppgave =
            oppgaveApiDao.finnOppgaveId(`fødselsnummer`)?.let { oppgaveId ->
                transaction.oppgaveRepository
                    .finn(oppgaveId)
                    ?.kanSeesAv(saksbehandler, tilgangsgrupper)
            } ?: true

        if (!harTilgangTilOppgave) {
            logg.warn("Saksbehandler mangler tilgang til aktiv oppgave på denne personen")
            return FetchPersonResult.Feil.ManglerTilgang
        }

        val snapshot =
            runCatching { snapshotService.hentSnapshot(`fødselsnummer`) }
                .getOrElse { e ->
                    sikkerlogg.error("feilet under henting av snapshot for {}", keyValue("fnr", `fødselsnummer`), e)
                    return FetchPersonResult.Feil.KlarteIkkeHente
                }?.takeUnless { it.second.arbeidsgivere.isEmpty() }
                ?: return FetchPersonResult.Feil.IkkeFunnet

        val (personinfo, personSnapshot) = snapshot

        return FetchPersonResult.Ok(
            ApiPerson(
                resolver =
                    ApiPersonResolver(
                        andreFødselsnummer =
                            personApiDao
                                .finnFødselsnumre(personEntity.aktørId)
                                .toSet()
                                .filterNot { fnr -> fnr == `fødselsnummer` }
                                .associateWith { fnr ->
                                    transaction.personPseudoIdDao.nyPersonPseudoId(
                                        identitetsnummer = Identitetsnummer.fraString(fnr),
                                    )
                                }.entries
                                .map { (fødselsnummer, pseudoId) -> Identitetsnummer.fraString(fødselsnummer) to pseudoId }
                                .toSet(),
                        snapshot = personSnapshot,
                        personinfo =
                            personinfo.copy(
                                unntattFraAutomatisering =
                                    stansAutomatiskBehandlinghåndterer.unntattFraAutomatiskGodkjenning(`fødselsnummer`),
                                fullmakt = vergemålApiDao.harFullmakt(`fødselsnummer`),
                                automatiskBehandlingStansetAvSaksbehandler =
                                    stansAutomatiskBehandlingSaksbehandlerDao.erStanset(`fødselsnummer`),
                            ),
                        personApiDao = personApiDao,
                        tildelingApiDao = tildelingApiDao,
                        arbeidsgiverApiDao = arbeidsgiverApiDao,
                        overstyringApiDao = overstyringApiDao,
                        risikovurderinger = risikovurderingApiDao.finnRisikovurderinger(`fødselsnummer`),
                        varselRepository = varselRepository,
                        oppgaveApiDao = oppgaveApiDao,
                        periodehistorikkApiDao = periodehistorikkApiDao,
                        notatDao = notatDao,
                        påVentApiDao = påVentApiDao,
                        apiOppgaveService = apiOppgaveService,
                        saksbehandlerMediator = saksbehandlerMediator,
                        sessionFactory = sessionFactory,
                        vedtakBegrunnelseDao = vedtakBegrunnelseDao,
                        annulleringRepository = annulleringRepository,
                        saksbehandlerRepository = saksbehandlerRepository,
                    ),
            ),
        )
    }
}
