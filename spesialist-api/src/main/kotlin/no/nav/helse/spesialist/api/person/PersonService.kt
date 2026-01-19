package no.nav.helse.spesialist.api.person

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.db.AnnulleringRepository
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
import no.nav.helse.spesialist.api.graphql.mapping.toApiReservasjon
import no.nav.helse.spesialist.api.graphql.query.FetchPersonResult
import no.nav.helse.spesialist.api.graphql.query.PersonoppslagService
import no.nav.helse.spesialist.api.graphql.resolvers.ApiPersonResolver
import no.nav.helse.spesialist.api.graphql.schema.ApiPerson
import no.nav.helse.spesialist.api.graphql.schema.ApiPersoninfo
import no.nav.helse.spesialist.api.snapshot.SnapshotService
import no.nav.helse.spesialist.application.KrrRegistrertStatusHenter
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.SaksbehandlerRepository
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.application.snapshot.SnapshotPerson
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

private sealed interface HentSnapshotResult {
    class Ok(
        val snapshot: Pair<ApiPersoninfo, SnapshotPerson>,
    ) : HentSnapshotResult

    sealed interface Feil : HentSnapshotResult {
        data object IkkeFunnet : Feil

        data object KlarteIkkeHente : Feil
    }
}

class PersonService(
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
    private val krrRegistrertStatusHenter: KrrRegistrertStatusHenter,
    private val sessionFactory: SessionFactory,
    private val vedtakBegrunnelseDao: VedtakBegrunnelseDao,
    private val stansAutomatiskBehandlingSaksbehandlerDao: StansAutomatiskBehandlingSaksbehandlerDao,
    private val annulleringRepository: AnnulleringRepository,
    private val saksbehandlerRepository: SaksbehandlerRepository,
) : PersonoppslagService {
    override fun finnesPersonMedFødselsnummer(fødselsnummer: String): Boolean = personApiDao.finnesPersonMedFødselsnummer(fødselsnummer)

    override fun fødselsnumreKnyttetTil(aktørId: String): Set<String> = personApiDao.finnFødselsnumre(aktørId).toSet()

    override fun fødselsnummerKnyttetTil(personPseudoId: PersonPseudoId): Identitetsnummer? =
        sessionFactory.transactionalSessionScope { session ->
            session.personPseudoIdDao.hentIdentitetsnummer(personPseudoId)
        }

    override suspend fun hentPerson(
        fødselsnummer: String,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
    ): FetchPersonResult {
        val personEntity =
            sessionFactory.transactionalSessionScope {
                it.personRepository.finn(Identitetsnummer.fraString(fødselsnummer))
            }
        if (personEntity == null) {
            return FetchPersonResult.Feil.IkkeFunnet
        }
        if (!personEntity.harDataNødvendigForVisning()) {
            sessionFactory.transactionalSessionScope { session ->
                if (!session.personKlargjoresDao.klargjøringPågår(fødselsnummer)) {
                    personhåndterer.klargjørPersonForVisning(fødselsnummer)
                    session.personKlargjoresDao.personKlargjøres(fødselsnummer)
                }
            }

            return FetchPersonResult.Feil.IkkeKlarTilVisning(personEntity.aktørId)
        }
        if (!personEntity.kanSeesAvSaksbehandlerMedGrupper(tilgangsgrupper)) {
            return FetchPersonResult.Feil.ManglerTilgang
        }

        val reservasjon = finnReservasjonsstatus(fødselsnummer)
        val snapshot =
            when (val snapshotResult = hentSnapshot(fødselsnummer)) {
                HentSnapshotResult.Feil.IkkeFunnet -> return FetchPersonResult.Feil.IkkeFunnet
                HentSnapshotResult.Feil.KlarteIkkeHente -> return FetchPersonResult.Feil.KlarteIkkeHente
                is HentSnapshotResult.Ok -> snapshotResult.snapshot
            }

        // Best effort for å finne ut om saksbehandler har tilgang til oppgaven som gjelder
        // Litt vanskelig å få pent så lenge vi har dynamisk resolving av resten, og tilsynelatende "mange" oppgaver
        val harTilgangTilOppgave =
            oppgaveApiDao.finnOppgaveId(fødselsnummer)?.let { oppgaveId ->
                sessionFactory.transactionalSessionScope {
                    it.oppgaveRepository
                        .finn(oppgaveId)
                        ?.kanSeesAv(saksbehandler, tilgangsgrupper)
                }
            } ?: true

        if (!harTilgangTilOppgave) {
            logg.warn("Saksbehandler mangler tilgang til aktiv oppgave på denne personen")
            return FetchPersonResult.Feil.ManglerTilgang
        }

        val fødselsnumrePseudoIdMap =
            sessionFactory.transactionalSessionScope { session ->
                fødselsnumreKnyttetTil(personEntity.aktørId).associateWith { fnr ->
                    session.personPseudoIdDao.nyPersonPseudoId(
                        identitetsnummer = Identitetsnummer.fraString(fnr),
                    )
                }
            }
        return person(fødselsnummer, fødselsnumrePseudoIdMap, snapshot, reservasjon)
    }

    private suspend fun person(
        fødselsnummer: String,
        fødselsnumrePseudoIdMap: Map<String, PersonPseudoId>,
        snapshot: Pair<ApiPersoninfo, SnapshotPerson>,
        reservasjon: Deferred<KrrRegistrertStatusHenter.KrrRegistrertStatus>,
    ): FetchPersonResult.Ok {
        val (personinfo, personSnapshot) = snapshot
        return FetchPersonResult.Ok(
            ApiPerson(
                resolver =
                    ApiPersonResolver(
                        personPseudoId = fødselsnumrePseudoIdMap.getValue(fødselsnummer),
                        andreFødselsnummer =
                            fødselsnumrePseudoIdMap.entries
                                .filterNot { (fnr, _) -> fnr == fødselsnummer }
                                .map { (fødselsnummer, pseudoId) -> Identitetsnummer.fraString(fødselsnummer) to pseudoId }
                                .toSet(),
                        snapshot = personSnapshot,
                        personinfo =
                            personinfo.copy(
                                reservasjon = reservasjon.await().toApiReservasjon(),
                                unntattFraAutomatisering =
                                    stansAutomatiskBehandlinghåndterer.unntattFraAutomatiskGodkjenning(fødselsnummer),
                                fullmakt = vergemålApiDao.harFullmakt(fødselsnummer),
                                automatiskBehandlingStansetAvSaksbehandler =
                                    stansAutomatiskBehandlingSaksbehandlerDao.erStanset(fødselsnummer),
                            ),
                        personApiDao = personApiDao,
                        tildelingApiDao = tildelingApiDao,
                        arbeidsgiverApiDao = arbeidsgiverApiDao,
                        overstyringApiDao = overstyringApiDao,
                        risikovurderinger = risikovurderingApiDao.finnRisikovurderinger(fødselsnummer),
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

    private fun finnReservasjonsstatus(fødselsnummer: String) =
        CoroutineScope(Dispatchers.IO).async {
            krrRegistrertStatusHenter.hentForPerson(fødselsnummer)
        }

    private fun hentSnapshot(fødselsnummer: String): HentSnapshotResult {
        val snapshot =
            try {
                snapshotService.hentSnapshot(fødselsnummer)
            } catch (e: Exception) {
                sikkerlogg.error("feilet under henting av snapshot for {}", keyValue("fnr", fødselsnummer), e)
                return HentSnapshotResult.Feil.KlarteIkkeHente
            } ?: return HentSnapshotResult.Feil.IkkeFunnet
        if (snapshot.second.arbeidsgivere.isEmpty()) {
            return HentSnapshotResult.Feil.IkkeFunnet
        }
        return HentSnapshotResult.Ok(snapshot)
    }
}
