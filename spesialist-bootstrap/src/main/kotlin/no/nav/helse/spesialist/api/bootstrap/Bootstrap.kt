package no.nav.helse.spesialist.api.bootstrap

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import no.nav.helse.bootstrap.Environment
import no.nav.helse.db.Repositories
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
    private val snapshotApiDao = repositories.snapshotApiDao
    private val personApiDao = repositories.personApiDao
    private val oppgaveApiDao = repositories.oppgaveApiDao
    private val periodehistorikkApiDao = repositories.periodehistorikkApiDao
    private val risikovurderingApiDao = repositories.risikovurderingApiDao
    private val tildelingApiDao = repositories.tildelingApiDao
    private val overstyringApiDao = repositories.overstyringApiDao
    private val arbeidsgiverApiDao = repositories.arbeidsgiverApiDao
    private val egenAnsattApiDao = repositories.egenAnsattApiDao
    private val notatApiDao = repositories.notatApiDao
    private val totrinnsvurderingApiDao = repositories.totrinnsvurderingApiDao
    private val apiVarselRepository = repositories.apiVarselRepository
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
