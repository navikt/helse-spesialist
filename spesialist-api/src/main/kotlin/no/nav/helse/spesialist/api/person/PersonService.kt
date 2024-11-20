package no.nav.helse.spesialist.api.person

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.bootstrap.Environment
import no.nav.helse.spesialist.api.Avviksvurderinghenter
import no.nav.helse.spesialist.api.Personhåndterer
import no.nav.helse.spesialist.api.SaksbehandlerTilganger
import no.nav.helse.spesialist.api.Saksbehandlerhåndterer
import no.nav.helse.spesialist.api.StansAutomatiskBehandlinghåndterer
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.egenAnsatt.EgenAnsattApiDao
import no.nav.helse.spesialist.api.graphql.query.FetchPersonResult
import no.nav.helse.spesialist.api.graphql.query.PersonoppslagService
import no.nav.helse.spesialist.api.graphql.schema.Person
import no.nav.helse.spesialist.api.graphql.schema.Personinfo
import no.nav.helse.spesialist.api.graphql.schema.Reservasjon
import no.nav.helse.spesialist.api.notat.NotatApiDao
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.oppgave.Oppgavehåndterer
import no.nav.helse.spesialist.api.overstyring.OverstyringApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkApiDao
import no.nav.helse.spesialist.api.påvent.PåVentApiDao
import no.nav.helse.spesialist.api.reservasjon.ReservasjonClient
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.saksbehandler.manglerTilgang
import no.nav.helse.spesialist.api.snapshot.SnapshotService
import no.nav.helse.spesialist.api.tildeling.TildelingApiDao
import no.nav.helse.spesialist.api.totrinnsvurdering.TotrinnsvurderingApiDao
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import no.nav.helse.spesialist.api.vergemål.VergemålApiDao
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLPerson
import org.slf4j.LoggerFactory

private sealed interface HentSnapshotResult {
    class Ok(val snapshot: Pair<Personinfo, GraphQLPerson>) : HentSnapshotResult

    sealed interface Feil : HentSnapshotResult {
        data object IkkeFunnet : Feil

        data object Ugyldig : Feil
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
    private val varselRepository: ApiVarselRepository,
    private val oppgaveApiDao: OppgaveApiDao,
    private val periodehistorikkApiDao: PeriodehistorikkApiDao,
    private val notatDao: NotatApiDao,
    private val totrinnsvurderingApiDao: TotrinnsvurderingApiDao,
    private val påVentApiDao: PåVentApiDao,
    private val avviksvurderinghenter: Avviksvurderinghenter,
    private val oppgavehåndterer: Oppgavehåndterer,
    private val saksbehandlerhåndterer: Saksbehandlerhåndterer,
    private val stansAutomatiskBehandlinghåndterer: StansAutomatiskBehandlinghåndterer,
    private val personhåndterer: Personhåndterer,
    private val snapshotService: SnapshotService,
    private val reservasjonClient: ReservasjonClient,
) : PersonoppslagService {
    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    private val env = Environment()

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
                HentSnapshotResult.Feil.Ugyldig -> return FetchPersonResult.Feil.UgyldigSnapshot
                is HentSnapshotResult.Ok -> snapshotResult.snapshot
            }

        return person(fødselsnummer, snapshot, tilganger, reservasjon)
    }

    private suspend fun person(
        fødselsnummer: String,
        snapshot: Pair<Personinfo, GraphQLPerson>,
        tilganger: SaksbehandlerTilganger,
        reservasjon: Deferred<Reservasjon?>,
    ): FetchPersonResult.Ok {
        val (personinfo, personSnapshot) = snapshot
        return FetchPersonResult.Ok(
            Person(
                snapshot = personSnapshot,
                personinfo =
                    personinfo.copy(
                        reservasjon = reservasjon.await(),
                        unntattFraAutomatisering = stansAutomatiskBehandlinghåndterer.unntattFraAutomatiskGodkjenning(fødselsnummer),
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
                tilganger = tilganger,
                oppgavehåndterer = oppgavehåndterer,
                saksbehandlerhåndterer = saksbehandlerhåndterer,
            ),
        )
    }

    private fun finnReservasjonsstatus(fødselsnummer: String) =
        if (env.erDev) {
            CompletableDeferred<Reservasjon?>().also { it.complete(null) }
        } else {
            CoroutineScope(Dispatchers.IO).async {
                reservasjonClient.hentReservasjonsstatus(fødselsnummer)
            }
        }

    private fun hentSnapshot(fødselsnummer: String): HentSnapshotResult {
        val snapshot =
            try {
                snapshotService.hentSnapshot(fødselsnummer)
            } catch (e: Exception) {
                sikkerlogg.error("feilet under henting av snapshot for {}", keyValue("fnr", fødselsnummer), e)
                return HentSnapshotResult.Feil.Ugyldig
            } ?: return HentSnapshotResult.Feil.IkkeFunnet
        return HentSnapshotResult.Ok(snapshot)
    }
}
