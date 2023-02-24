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
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.oppgave.OppgaveDao
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.varsel.ActualVarselRepository
import no.nav.helse.modell.vedtaksperiode.ActualGenerasjonRepository
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
    dataSource: DataSource
) {
    private val warningDao = WarningDao(dataSource)
    private val vedtakDao = VedtakDao(dataSource)
    private val opptegnelseDao = OpptegnelseDao(dataSource)
    private val overstyringDao = OverstyringDao(dataSource)

    private val godkjenningMediator = GodkjenningMediator(warningDao, vedtakDao, opptegnelseDao, ActualVarselRepository(dataSource), ActualGenerasjonRepository(dataSource))
    private val oppgaveMediator = OppgaveMediator(OppgaveDao(dataSource), TildelingDao(dataSource), ReservasjonDao(dataSource),
        opptegnelseDao
    )
    private val overstyringMediator = OverstyringMediator(testRapid)
    private val snapshotMediator = SnapshotMediator(SnapshotApiDao(dataSource), snapshotClient)
    private val automatisering = Automatisering(
        warningDao,
        RisikovurderingDao(dataSource),
        AutomatiseringDao(dataSource),
        ÅpneGosysOppgaverDao(dataSource),
        EgenAnsattDao(dataSource),
        VergemålDao(dataSource),
        PersonDao(dataSource),
        vedtakDao,
        OverstyringDao(dataSource),
        snapshotMediator
    ) { false }

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
            opptegnelseDao = opptegnelseDao,
            oppgaveMediator = oppgaveMediator,
            hendelsefabrikk = hendelsefabrikk
        )
    }

    internal fun overstyringstyperForVedtaksperiode(vedtaksperiodeId: UUID) = overstyringDao.finnOverstyringerMedTypeForVedtaksperiode(vedtaksperiodeId)
}