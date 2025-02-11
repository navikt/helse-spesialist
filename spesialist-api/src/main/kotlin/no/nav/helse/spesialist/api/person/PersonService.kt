package no.nav.helse.spesialist.api.person

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.bootstrap.Environment
import no.nav.helse.db.api.ArbeidsgiverApiDao
import no.nav.helse.db.api.EgenAnsattApiDao
import no.nav.helse.db.api.NotatApiDao
import no.nav.helse.db.api.OppgaveApiDao
import no.nav.helse.db.api.OverstyringApiDao
import no.nav.helse.db.api.PeriodehistorikkApiDao
import no.nav.helse.db.api.PersonApiDao
import no.nav.helse.db.api.PåVentApiDao
import no.nav.helse.db.api.RisikovurderingApiDao
import no.nav.helse.db.api.TildelingApiDao
import no.nav.helse.db.api.TotrinnsvurderingApiDao
import no.nav.helse.db.api.VarselApiRepository
import no.nav.helse.db.api.VergemålApiDao
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.spesialist.api.Avviksvurderinghenter
import no.nav.helse.spesialist.api.Personhåndterer
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.StansAutomatiskBehandlinghåndterer
import no.nav.helse.spesialist.api.graphql.mapping.toApiReservasjon
import no.nav.helse.spesialist.api.graphql.query.FetchPersonResult
import no.nav.helse.spesialist.api.graphql.query.PersonoppslagService
import no.nav.helse.spesialist.api.graphql.resolvers.ApiPersonResolver
import no.nav.helse.spesialist.api.graphql.schema.ApiPerson
import no.nav.helse.spesialist.api.graphql.schema.ApiPersoninfo
import no.nav.helse.spesialist.api.saksbehandler.manglerTilgang
import no.nav.helse.spesialist.api.snapshot.SnapshotService
import no.nav.helse.spesialist.application.Reservasjonshenter
import no.nav.helse.spesialist.application.Reservasjonshenter.ReservasjonDto
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson
import org.slf4j.LoggerFactory

private sealed interface HentSnapshotResult {
    class Ok(val snapshot: Pair<ApiPersoninfo, GraphQLPerson>) : HentSnapshotResult

    sealed interface Feil : HentSnapshotResult {
        data object IkkeFunnet : Feil

        data object KlarteIkkeHente : Feil
    }
}

class PersonService(
    private val personApiDao: PersonApiDao,
    private val egenAnsattApiDao: EgenAnsattApiDao,
    private val vergemålApiDao: VergemålApiDao,
    private val tildelingApiDao: TildelingApiDao,
    private val arbeidsgiverApiDao: ArbeidsgiverApiDao,
    private val overstyringApiDao: OverstyringApiDao,
    private val risikovurderingApiDao: RisikovurderingApiDao,
    private val varselRepository: VarselApiRepository,
    private val oppgaveApiDao: OppgaveApiDao,
    private val periodehistorikkApiDao: PeriodehistorikkApiDao,
    private val notatDao: NotatApiDao,
    private val totrinnsvurderingApiDao: TotrinnsvurderingApiDao,
    private val påVentApiDao: PåVentApiDao,
    private val avviksvurderinghenter: Avviksvurderinghenter,
    private val apiOppgaveService: ApiOppgaveService,
    private val saksbehandlerhåndterer: Saksbehandlerhåndterer,
    private val stansAutomatiskBehandlinghåndterer: StansAutomatiskBehandlinghåndterer,
    private val personhåndterer: Personhåndterer,
    private val snapshotService: SnapshotService,
    private val reservasjonshenter: Reservasjonshenter,
    private val env: Environment,
) : PersonoppslagService {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    override fun finnesPersonMedFødselsnummer(fødselsnummer: String): Boolean {
        return personApiDao.finnesPersonMedFødselsnummer(fødselsnummer)
    }

    override fun fødselsnumreKnyttetTil(aktørId: String): Set<String> {
        return personApiDao.finnFødselsnumre(aktørId).toSet()
    }

    override suspend fun hentPerson(
        fødselsnummer: String,
        tilganger: SaksbehandlerTilganger,
    ): FetchPersonResult {
        val aktørId = personApiDao.finnAktørId(fødselsnummer)
        if (!personApiDao.harDataNødvendigForVisning(fødselsnummer)) {
            if (!personApiDao.klargjøringPågår(fødselsnummer)) {
                personhåndterer.klargjørPersonForVisning(fødselsnummer)
                personApiDao.personKlargjøres(fødselsnummer)
            }

            return FetchPersonResult.Feil.IkkeKlarTilVisning(aktørId)
        }
        if (manglerTilgang(egenAnsattApiDao, personApiDao, fødselsnummer, tilganger)) return FetchPersonResult.Feil.ManglerTilgang

        val reservasjon = finnReservasjonsstatus(fødselsnummer)
        val snapshot =
            when (val snapshotResult = hentSnapshot(fødselsnummer)) {
                HentSnapshotResult.Feil.IkkeFunnet -> return FetchPersonResult.Feil.IkkeFunnet
                HentSnapshotResult.Feil.KlarteIkkeHente -> return FetchPersonResult.Feil.KlarteIkkeHente
                is HentSnapshotResult.Ok -> snapshotResult.snapshot
            }

        return person(fødselsnummer, snapshot, reservasjon)
    }

    private suspend fun person(
        fødselsnummer: String,
        snapshot: Pair<ApiPersoninfo, GraphQLPerson>,
        reservasjon: Deferred<ReservasjonDto?>,
    ): FetchPersonResult.Ok {
        val (personinfo, personSnapshot) = snapshot
        return FetchPersonResult.Ok(
            ApiPerson(
                resolver =
                    ApiPersonResolver(
                        snapshot = personSnapshot,
                        personinfo =
                            personinfo.copy(
                                reservasjon = reservasjon.await()?.toApiReservasjon(),
                                unntattFraAutomatisering =
                                    stansAutomatiskBehandlinghåndterer.unntattFraAutomatiskGodkjenning(fødselsnummer),
                                fullmakt = vergemålApiDao.harFullmakt(fødselsnummer),
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
                        totrinnsvurderingApiDao = totrinnsvurderingApiDao,
                        påVentApiDao = påVentApiDao,
                        avviksvurderinghenter = avviksvurderinghenter,
                        apiOppgaveService = apiOppgaveService,
                        saksbehandlerhåndterer = saksbehandlerhåndterer,
                    ),
            ),
        )
    }

    private fun finnReservasjonsstatus(fødselsnummer: String) =
        if (env.erDev) {
            CompletableDeferred<ReservasjonDto?>().also { it.complete(null) }
        } else {
            CoroutineScope(Dispatchers.IO).async {
                reservasjonshenter.hentForPerson(fødselsnummer)
            }
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
