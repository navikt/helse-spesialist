import no.nav.helse.SpeilTilgangsgrupper
import no.nav.helse.db.OpptegnelseDao
import no.nav.helse.db.PgDialogDao
import no.nav.helse.db.PgAvviksvurderingDao
import no.nav.helse.db.PgNotatDao
import no.nav.helse.db.PgOppgaveDao
import no.nav.helse.db.PgPeriodehistorikkDao
import no.nav.helse.db.PgTotrinnsvurderingDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.db.StansAutomatiskBehandlingDao
import no.nav.helse.db.TildelingDao
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.Kommandofabrikk
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.mediator.Subsumsjonsmelder
import no.nav.helse.mediator.meldinger.PoisonPills
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.stoppautomatiskbehandling.StansAutomatiskBehandlingMediator
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingService
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
    private val opptegnelseDao = OpptegnelseDao(dataSource)
    private val oppgaveDao = PgOppgaveDao(dataSource)
    private val historikkinnslagRepository = PgPeriodehistorikkDao(dataSource)
    private val overstyringDao = OverstyringDao(dataSource)
    private val totrinnsvurderingDao = PgTotrinnsvurderingDao(dataSource)
    private val saksbehandlerDao = SaksbehandlerDao(dataSource)
    private val tildelingDao = TildelingDao(dataSource)
    private val avviksvurderingDao = PgAvviksvurderingDao(dataSource)
    private val notatDao = PgNotatDao(dataSource)
    private val dialogDao = PgDialogDao(dataSource)

    private val stansAutomatiskBehandlingMediator =
        StansAutomatiskBehandlingMediator(
            StansAutomatiskBehandlingDao(dataSource),
            historikkinnslagRepository,
            oppgaveDao,
            notatDao,
            dialogDao,
        ) { Subsumsjonsmelder("versjonAvKode", testRapid) }

    private val godkjenningMediator = GodkjenningMediator(opptegnelseDao)
    private val tilgangsgrupper = SpeilTilgangsgrupper(testEnv)
    private val oppgaveService =
        OppgaveService(
            oppgaveDao = PgOppgaveDao(dataSource),
            tildelingRepository = tildelingDao,
            reservasjonRepository = ReservasjonDao(dataSource),
            opptegnelseRepository = opptegnelseDao,
            totrinnsvurderingDao = totrinnsvurderingDao,
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
            totrinnsvurderingService =
                TotrinnsvurderingService(
                    totrinnsvurderingDao = totrinnsvurderingDao,
                    oppgaveDao = oppgaveDao,
                    periodehistorikkDao = historikkinnslagRepository,
                    dialogDao = dialogDao,
                ),
        )
    private val stikkprøver =
        object : Stikkprøver {
            override fun utsFlereArbeidsgivereFørstegangsbehandling() = false

            override fun utsFlereArbeidsgivereForlengelse() = false

            override fun utsEnArbeidsgiverFørstegangsbehandling() = false

            override fun utsEnArbeidsgiverForlengelse() = false

            override fun fullRefusjonFlereArbeidsgivereFørstegangsbehandling() = false

            override fun fullRefusjonFlereArbeidsgivereForlengelse() = false

            override fun fullRefusjonEnArbeidsgiver() = false
        }

    private val kommandofabrikk =
        Kommandofabrikk(
            dataSource = dataSource,
            oppgaveService = { oppgaveService },
            godkjenningMediator = godkjenningMediator,
            subsumsjonsmelderProvider = { Subsumsjonsmelder("versjonAvKode", testRapid) },
            stikkprøver = stikkprøver,
        )

    init {
        MeldingMediator(
            dataSource = dataSource,
            rapidsConnection = testRapid,
            kommandofabrikk = kommandofabrikk,
            avviksvurderingDao = avviksvurderingDao,
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
