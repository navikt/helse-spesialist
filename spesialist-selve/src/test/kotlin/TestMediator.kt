import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.Hendelsefabrikk
import no.nav.helse.mediator.OverstyringMediator
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringDao
import no.nav.helse.modell.automatisering.Stikkprøver
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.varsel.ActualVarselRepository
import no.nav.helse.modell.vergemal.VergemålDao
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.reservasjon.ReservasjonDao
import no.nav.helse.spesialist.api.snapshot.SnapshotApiDao
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import no.nav.helse.spesialist.api.snapshot.SnapshotMediator
import no.nav.helse.spesialist.api.tildeling.TildelingDao


internal class TestMediator(
    testRapid: TestRapid,
    snapshotClient: SnapshotClient,
    dataSource: DataSource,
) {
    private val warningDao = WarningDao(dataSource)
    private val vedtakDao = VedtakDao(dataSource)
    private val opptegnelseDao = OpptegnelseDao(dataSource)
    private val overstyringDao = OverstyringDao(dataSource)

    private val godkjenningMediator = GodkjenningMediator(
        warningDao,
        vedtakDao,
        opptegnelseDao,
        ActualVarselRepository(dataSource)
    )
    private val oppgaveMediator = OppgaveMediator(
        oppgaveDao = OppgaveDao(dataSource),
        tildelingDao = TildelingDao(dataSource),
        reservasjonDao = ReservasjonDao(dataSource),
        opptegnelseDao = opptegnelseDao
    )
    private val overstyringMediator = OverstyringMediator(testRapid)
    private val snapshotMediator = SnapshotMediator(SnapshotApiDao(dataSource), snapshotClient)
    private val automatisering = Automatisering(
        warningDao = warningDao,
        risikovurderingDao = RisikovurderingDao(dataSource),
        automatiseringDao = AutomatiseringDao(dataSource),
        åpneGosysOppgaverDao = ÅpneGosysOppgaverDao(dataSource),
        egenAnsattDao = EgenAnsattDao(dataSource),
        vergemålDao = VergemålDao(dataSource),
        personDao = PersonDao(dataSource),
        vedtakDao = vedtakDao,
        overstyringDao = OverstyringDao(dataSource),
        snapshotMediator = snapshotMediator,
        stikkprøver = object : Stikkprøver {
            override fun fullRefusjon() = false
            override fun uts() = false
        }
    )

    private val hendelsefabrikk = Hendelsefabrikk(
        dataSource = dataSource,
        snapshotClient = snapshotClient,
        oppgaveMediator = oppgaveMediator,
        godkjenningMediator = godkjenningMediator,
        automatisering = automatisering,
        overstyringMediator = overstyringMediator,
        snapshotMediator = snapshotMediator
    )

    init {
        HendelseMediator(
            dataSource = dataSource,
            rapidsConnection = testRapid,
            oppgaveMediator = oppgaveMediator,
            hendelsefabrikk = hendelsefabrikk
        )
    }

    internal fun overstyringstyperForVedtaksperiode(vedtaksperiodeId: UUID) =
        overstyringDao.finnOverstyringerMedTypeForVedtaksperiode(vedtaksperiodeId)
}
