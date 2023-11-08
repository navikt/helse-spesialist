import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.Tilgangsgrupper
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.db.TildelingDao
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.Hendelsefabrikk
import no.nav.helse.mediator.OverstyringMediator
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.mediator.oppgave.OppgaveMediator
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringDao
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.varsel.DefinisjonDao
import no.nav.helse.modell.varsel.Varselmelder
import no.nav.helse.modell.vedtaksperiode.GenerasjonDao
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import no.nav.helse.spesialist.api.snapshot.SnapshotApiDao
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import no.nav.helse.spesialist.api.snapshot.SnapshotMediator
import no.nav.helse.testEnv

internal class TestMediator(
    testRapid: TestRapid,
    snapshotClient: SnapshotClient,
    dataSource: DataSource,
) {
    private val vedtakDao = VedtakDao(dataSource)
    private val opptegnelseDao = OpptegnelseDao(dataSource)
    private val oppgaveDao = OppgaveDao(dataSource)
    private val utbetalingDao = UtbetalingDao(dataSource)
    private val overstyringDao = OverstyringDao(dataSource)
    private val hendelseDao = HendelseDao(dataSource)
    private val generasjonDao = GenerasjonDao(dataSource)
    private val totrinnsvurderingDao = TotrinnsvurderingDao(dataSource)
    private val saksbehandlerDao = SaksbehandlerDao(dataSource)
    private val tildelingDao = TildelingDao(dataSource)

    private val godkjenningMediator = GodkjenningMediator(
        vedtakDao,
        opptegnelseDao,
        oppgaveDao,
        utbetalingDao,
        hendelseDao,
    )
    private val tilgangsgrupper = Tilgangsgrupper(testEnv)
    private val oppgaveMediator = OppgaveMediator(
        hendelseDao = hendelseDao,
        oppgaveDao = OppgaveDao(dataSource),
        tildelingDao = tildelingDao,
        reservasjonDao = ReservasjonDao(dataSource),
        opptegnelseDao = opptegnelseDao,
        totrinnsvurderingRepository = totrinnsvurderingDao,
        saksbehandlerRepository = saksbehandlerDao,
        rapidsConnection = testRapid,
        tilgangskontroll = TilgangskontrollForTestHarIkkeTilgang,
        tilgangsgrupper = tilgangsgrupper
    )
    private val saksbehandlerMediator = SaksbehandlerMediator(dataSource, "versjonAvKode", testRapid, oppgaveMediator, tilgangsgrupper)
    private val overstyringMediator = OverstyringMediator(testRapid)
    private val snapshotMediator = SnapshotMediator(SnapshotApiDao(dataSource), snapshotClient)
    private val automatisering = Automatisering(
        risikovurderingDao = RisikovurderingDao(dataSource),
        automatiseringDao = AutomatiseringDao(dataSource),
        åpneGosysOppgaverDao = ÅpneGosysOppgaverDao(dataSource),
        vergemålDao = VergemålDao(dataSource),
        personDao = PersonDao(dataSource),
        vedtakDao = vedtakDao,
        overstyringDao = OverstyringDao(dataSource),
        stikkprøver = object : Stikkprøver {
            override fun utsFlereArbeidsgivereFørstegangsbehandling() = false
            override fun utsFlereArbeidsgivereForlengelse() = false
            override fun utsEnArbeidsgiverFørstegangsbehandling() = false
            override fun utsEnArbeidsgiverForlengelse() = false
            override fun fullRefusjonFlereArbeidsgivereFørstegangsbehandling() = false
            override fun fullRefusjonFlereArbeidsgivereForlengelse() = false
            override fun fullRefusjonEnArbeidsgiver() = false
        },
        hendelseDao = hendelseDao,
        generasjonDao = generasjonDao,
    )

    private val hendelsefabrikk = Hendelsefabrikk(
        dataSource = dataSource,
        snapshotClient = snapshotClient,
        oppgaveMediator = { oppgaveMediator },
        godkjenningMediator = godkjenningMediator,
        automatisering = automatisering,
        overstyringMediator = overstyringMediator,
        snapshotMediator = snapshotMediator,
        versjonAvKode = "versjonAvKode",
        definisjonDao = DefinisjonDao(dataSource),
        varselmelder = Varselmelder(rapidsConnection = testRapid)
    )

    init {
        HendelseMediator(
            dataSource = dataSource,
            rapidsConnection = testRapid,
            godkjenningMediator = godkjenningMediator,
            hendelsefabrikk = hendelsefabrikk
        )
    }

    internal fun overstyringstyperForVedtaksperiode(vedtaksperiodeId: UUID) =
        overstyringDao.finnOverstyringerMedTypeForVedtaksperiode(vedtaksperiodeId)

    internal fun håndter(handling: HandlingFraApi, saksbehandlerFraApi: SaksbehandlerFraApi) {
        saksbehandlerMediator.håndter(handling, saksbehandlerFraApi)
    }
}
