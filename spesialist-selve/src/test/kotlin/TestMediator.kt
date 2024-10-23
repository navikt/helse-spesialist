import no.nav.helse.SpeilTilgangsgrupper
import no.nav.helse.db.AvviksvurderingDao
import no.nav.helse.db.NotatDao
import no.nav.helse.db.OpptegnelseDao
import no.nav.helse.db.PgHistorikkinnslagRepository
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.db.StansAutomatiskBehandlingDao
import no.nav.helse.db.TildelingDao
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.Kommandofabrikk
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.mediator.Subsumsjonsmelder
import no.nav.helse.mediator.meldinger.PoisonPills
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.MeldingDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringDao
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMediator
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import no.nav.helse.testEnv
import java.util.UUID
import javax.sql.DataSource

internal class TestMediator(
    testRapid: TestRapid,
    dataSource: DataSource,
) {
    private val vedtakDao = VedtakDao(dataSource)
    private val opptegnelseDao = OpptegnelseDao(dataSource)
    private val oppgaveDao = OppgaveDao(dataSource)
    private val pgHistorikkinnslagRepository = PgHistorikkinnslagRepository(dataSource)
    private val overstyringDao = OverstyringDao(dataSource)
    private val meldingDao = MeldingDao(dataSource)
    private val generasjonDao = GenerasjonDao(dataSource)
    private val totrinnsvurderingDao = TotrinnsvurderingDao(dataSource)
    private val saksbehandlerDao = SaksbehandlerDao(dataSource)
    private val tildelingDao = TildelingDao(dataSource)
    private val avviksvurderingDao = AvviksvurderingDao(dataSource)
    private val egenAnsattDao = EgenAnsattDao(dataSource)
    private val notatDao = NotatDao(dataSource)

    private val stansAutomatiskBehandlingMediator =
        StansAutomatiskBehandlingMediator(
            StansAutomatiskBehandlingDao(dataSource),
            pgHistorikkinnslagRepository,
            oppgaveDao,
            notatDao,
        ) { Subsumsjonsmelder("versjonAvKode", testRapid) }

    private val godkjenningMediator = GodkjenningMediator(opptegnelseDao)
    private val tilgangsgrupper = SpeilTilgangsgrupper(testEnv)
    private val oppgaveService =
        OppgaveService(
            meldingRepository = meldingDao,
            oppgaveRepository = OppgaveDao(dataSource),
            tildelingRepository = tildelingDao,
            reservasjonRepository = ReservasjonDao(dataSource),
            opptegnelseRepository = opptegnelseDao,
            totrinnsvurderingRepository = totrinnsvurderingDao,
            saksbehandlerRepository = saksbehandlerDao,
            rapidsConnection = testRapid,
            tilgangskontroll = TilgangskontrollForTestHarIkkeTilgang,
            tilgangsgrupper = tilgangsgrupper,
        )
    private val saksbehandlerMediator =
        SaksbehandlerMediator(
            dataSource,
            "versjonAvKode",
            testRapid,
            oppgaveService,
            tilgangsgrupper,
            stansAutomatiskBehandlingMediator,
        )
    private val automatisering =
        Automatisering(
            risikovurderingRepository = RisikovurderingDao(dataSource),
            stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlingMediator,
            automatiseringRepository = AutomatiseringDao(dataSource),
            åpneGosysOppgaverRepository = ÅpneGosysOppgaverDao(dataSource),
            vergemålRepository = VergemålDao(dataSource),
            personRepository = PersonDao(dataSource),
            vedtakRepository = vedtakDao,
            overstyringRepository = OverstyringDao(dataSource),
            stikkprøver =
                object : Stikkprøver {
                    override fun utsFlereArbeidsgivereFørstegangsbehandling() = false

                    override fun utsFlereArbeidsgivereForlengelse() = false

                    override fun utsEnArbeidsgiverFørstegangsbehandling() = false

                    override fun utsEnArbeidsgiverForlengelse() = false

                    override fun fullRefusjonFlereArbeidsgivereFørstegangsbehandling() = false

                    override fun fullRefusjonFlereArbeidsgivereForlengelse() = false

                    override fun fullRefusjonEnArbeidsgiver() = false
                },
            meldingRepository = meldingDao,
            generasjonRepository = generasjonDao,
            egenAnsattRepository = egenAnsattDao,
        )

    private val kommandofabrikk =
        Kommandofabrikk(
            dataSource = dataSource,
            oppgaveService = { oppgaveService },
            godkjenningMediator = godkjenningMediator,
            automatisering = automatisering,
        )

    init {
        MeldingMediator(
            dataSource = dataSource,
            rapidsConnection = testRapid,
            kommandofabrikk = kommandofabrikk,
            avviksvurderingDao = avviksvurderingDao,
            stansAutomatiskBehandlingMediator = stansAutomatiskBehandlingMediator,
            poisonPills = PoisonPills(emptyMap()),
        )
    }

    internal fun overstyringstyperForVedtaksperiode(vedtaksperiodeId: UUID) =
        overstyringDao.finnOverstyringerMedTypeForVedtaksperiode(vedtaksperiodeId)

    internal fun håndter(
        handling: HandlingFraApi,
        saksbehandlerFraApi: SaksbehandlerFraApi,
    ) {
        saksbehandlerMediator.håndter(handling, saksbehandlerFraApi)
    }
}
