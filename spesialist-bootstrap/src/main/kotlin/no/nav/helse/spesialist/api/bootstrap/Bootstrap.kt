package no.nav.helse.spesialist.api.bootstrap

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import no.nav.helse.bootstrap.Environment
import no.nav.helse.db.Repositories
import no.nav.helse.db.api.PgApiVarselRepository
import no.nav.helse.db.api.PgArbeidsgiverApiDao
import no.nav.helse.db.api.PgNotatApiDao
import no.nav.helse.db.api.PgOppgaveApiDao
import no.nav.helse.db.api.PgOverstyringApiDao
import no.nav.helse.db.api.PgPeriodehistorikkApiDao
import no.nav.helse.db.api.PgPersonApiDao
import no.nav.helse.db.api.PgRisikovurderingApiDao
import no.nav.helse.db.api.PgSnapshotApiDao
import no.nav.helse.db.api.PgTildelingApiDao
import no.nav.helse.db.api.PgTotrinnsvurderingApiDao
import no.nav.helse.spesialist.api.AzureConfig
import no.nav.helse.spesialist.api.azureAdAppAuthentication
import no.nav.helse.spesialist.api.graphql.graphQLApi
import no.nav.helse.spesialist.api.reservasjon.ReservasjonClient
import no.nav.helse.spesialist.api.snapshot.SnapshotService
import no.nav.helse.spesialist.api.websockets.webSocketsApi
import javax.sql.DataSource

class Bootstrap(
    dataSource: DataSource,
    repositories: Repositories,
    private val avhengigheter: ApiAvhengigheter,
    private val reservasjonClient: ReservasjonClient,
    private val tilgangsgrupper: Tilgangsgrupper,
) {
    private val snapshotApiDao = PgSnapshotApiDao(dataSource)
    private val personApiDao = PgPersonApiDao(dataSource)
    private val oppgaveApiDao = PgOppgaveApiDao(dataSource)
    private val periodehistorikkApiDao = PgPeriodehistorikkApiDao(dataSource)
    private val risikovurderingApiDao = PgRisikovurderingApiDao(dataSource)
    private val tildelingApiDao = PgTildelingApiDao(dataSource)
    private val overstyringApiDao = PgOverstyringApiDao(dataSource)
    private val arbeidsgiverApiDao = PgArbeidsgiverApiDao(dataSource)
    private val egenAnsattApiDao = repositories.egenAnsattApiDao
    private val notatApiDao = PgNotatApiDao(dataSource)
    private val totrinnsvurderingApiDao = PgTotrinnsvurderingApiDao(dataSource)
    private val apiVarselRepository = PgApiVarselRepository(dataSource)
    private val påVentApiDao = repositories.påVentApiDao
    private val vergemålApiDao = repositories.vergemålApiDao

    fun ktorApp(
        application: Application,
        azureConfig: AzureConfig,
        env: Environment,
    ) = application.apply {
        installPlugins()
        azureAdAppAuthentication(azureConfig, env)
        graphQLApi(
            personApiDao = personApiDao,
            egenAnsattApiDao = egenAnsattApiDao,
            tildelingApiDao = tildelingApiDao,
            arbeidsgiverApiDao = arbeidsgiverApiDao,
            overstyringApiDao = overstyringApiDao,
            risikovurderingApiDao = risikovurderingApiDao,
            varselRepository = apiVarselRepository,
            oppgaveApiDao = oppgaveApiDao,
            periodehistorikkApiDao = periodehistorikkApiDao,
            notatDao = notatApiDao,
            totrinnsvurderingApiDao = totrinnsvurderingApiDao,
            påVentApiDao = påVentApiDao,
            vergemålApiDao = vergemålApiDao,
            reservasjonClient = reservasjonClient,
            avviksvurderinghenter = avhengigheter.avviksvurderinghenter,
            skjermedePersonerGruppeId = tilgangsgrupper.skjermedePersonerGruppeId,
            kode7Saksbehandlergruppe = tilgangsgrupper.kode7GruppeId,
            beslutterGruppeId = tilgangsgrupper.beslutterGruppeId,
            snapshotService = SnapshotService(snapshotApiDao, avhengigheter.snapshotClient),
            behandlingsstatistikkMediator = avhengigheter.behandlingstatistikk,
            saksbehandlerhåndterer = avhengigheter.saksbehandlerhåndtererProvider(),
            oppgavehåndterer = avhengigheter.oppgavehåndtererProvider(),
            totrinnsvurderinghåndterer = avhengigheter.totrinnsvurderinghåndterer(),
            godkjenninghåndterer = avhengigheter.godkjenninghåndtererProvider(),
            personhåndterer = avhengigheter.personhåndtererProvider(),
            dokumenthåndterer = avhengigheter.dokumenthåndtererProvider(),
            stansAutomatiskBehandlinghåndterer = avhengigheter.stansAutomatiskBehandlinghåndterer(),
        )

        routing {
            webSocketsApi()
            debugMinneApi()
        }
    }
}
