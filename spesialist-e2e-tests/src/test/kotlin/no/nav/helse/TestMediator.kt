package no.nav.helse

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.db.DBRepositories
import no.nav.helse.db.OpptegnelseDao
import no.nav.helse.db.PgDialogDao
import no.nav.helse.db.PgNotatDao
import no.nav.helse.db.PgOppgaveDao
import no.nav.helse.db.PgPeriodehistorikkDao
import no.nav.helse.db.PgTotrinnsvurderingDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.SaksbehandlerDao
import no.nav.helse.db.StansAutomatiskBehandlingDao
import no.nav.helse.db.TildelingDao
import no.nav.helse.kafka.MessageContextMeldingPubliserer
import no.nav.helse.kafka.RiverSetup
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
import no.nav.helse.spesialist.api.bootstrap.SpeilTilgangsgrupper
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.api.saksbehandler.handlinger.HandlingFraApi
import no.nav.helse.util.TilgangskontrollForTestHarIkkeTilgang
import no.nav.helse.util.testEnv
import java.util.UUID
import javax.sql.DataSource

internal class TestMediator(
    testRapid: TestRapid,
    dataSource: DataSource,
) {
    private val repositories = DBRepositories(dataSource)
    private val opptegnelseDao = OpptegnelseDao(dataSource)
    private val oppgaveDao = PgOppgaveDao(dataSource)
    private val historikkinnslagRepository = PgPeriodehistorikkDao(dataSource)
    private val overstyringDao = OverstyringDao(dataSource)
    private val totrinnsvurderingDao = PgTotrinnsvurderingDao(dataSource)
    private val saksbehandlerDao = SaksbehandlerDao(dataSource)
    private val tildelingDao = TildelingDao(dataSource)
    private val notatDao = PgNotatDao(dataSource)
    private val dialogDao = PgDialogDao(dataSource)
    private val annulleringRepository = repositories.annulleringRepository
    private val meldingPubliserer = MessageContextMeldingPubliserer(testRapid)

    private val stansAutomatiskBehandlingMediator =
        StansAutomatiskBehandlingMediator(
            StansAutomatiskBehandlingDao(dataSource),
            historikkinnslagRepository,
            oppgaveDao,
            notatDao,
            dialogDao,
        ) { Subsumsjonsmelder("versjonAvKode", meldingPubliserer) }

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
            meldingPubliserer = meldingPubliserer,
            tilgangskontroll = TilgangskontrollForTestHarIkkeTilgang,
            tilgangsgrupper = tilgangsgrupper,
        )

    private val saksbehandlerMediator =
        SaksbehandlerMediator(
            dataSource,
            repositories,
            "versjonAvKode",
            meldingPubliserer,
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
            annulleringRepository = annulleringRepository,
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
            repositories = repositories,
            oppgaveService = { oppgaveService },
            godkjenningMediator = godkjenningMediator,
            subsumsjonsmelderProvider = { Subsumsjonsmelder("versjonAvKode", meldingPubliserer) },
            stikkprøver = stikkprøver,
        )


    init {
        val meldingMediator = MeldingMediator(
            dataSource = dataSource,
            repositories = repositories,
            publiserer = meldingPubliserer,
            kommandofabrikk = kommandofabrikk,
            poisonPills = PoisonPills(emptyMap()),
        )
        RiverSetup(dataSource, testRapid, meldingMediator).setUp()
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
