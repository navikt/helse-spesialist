package no.nav.helse.spesialist.api.bootstrap

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import no.nav.helse.bootstrap.Environment
import no.nav.helse.db.Daos
import no.nav.helse.db.SessionFactory
import no.nav.helse.spesialist.api.AzureConfig
import no.nav.helse.spesialist.api.azureAdAppAuthentication
import no.nav.helse.spesialist.api.graphql.graphQLApi
import no.nav.helse.spesialist.api.snapshot.SnapshotService
import no.nav.helse.spesialist.api.websockets.webSocketsApi
import no.nav.helse.spesialist.application.Reservasjonshenter

class Bootstrap(
    private val daos: Daos,
    private val sessionFactory: SessionFactory,
    private val avhengigheter: ApiAvhengigheter,
    private val reservasjonshenter: Reservasjonshenter,
    private val tilgangsgrupper: Tilgangsgrupper,
) {
    fun ktorApp(
        application: Application,
        azureConfig: AzureConfig,
        env: Environment,
    ) = application.apply {
        installPlugins()
        azureAdAppAuthentication(azureConfig, env)
        graphQLApi(
            sessionFactory = sessionFactory,
            personApiDao = daos.personApiDao,
            egenAnsattApiDao = daos.egenAnsattApiDao,
            tildelingApiDao = daos.tildelingApiDao,
            arbeidsgiverApiDao = daos.arbeidsgiverApiDao,
            overstyringApiDao = daos.overstyringApiDao,
            risikovurderingApiDao = daos.risikovurderingApiDao,
            varselRepository = daos.varselApiRepository,
            oppgaveApiDao = daos.oppgaveApiDao,
            periodehistorikkApiDao = daos.periodehistorikkApiDao,
            notatDao = daos.notatApiDao,
            totrinnsvurderingApiDao = daos.totrinnsvurderingApiDao,
            påVentApiDao = daos.påVentApiDao,
            vergemålApiDao = daos.vergemålApiDao,
            reservasjonshenter = reservasjonshenter,
            skjermedePersonerGruppeId = tilgangsgrupper.skjermedePersonerGruppeId,
            kode7Saksbehandlergruppe = tilgangsgrupper.kode7GruppeId,
            beslutterGruppeId = tilgangsgrupper.beslutterGruppeId,
            snapshotService = SnapshotService(daos.personinfoDao, avhengigheter.snapshothenter),
            behandlingsstatistikkMediator = avhengigheter.behandlingstatistikk,
            saksbehandlerMediator = avhengigheter.saksbehandlerMediatorProvider(),
            apiOppgaveService = avhengigheter.apiOppgaveServiceProvider(),
            godkjenninghåndterer = avhengigheter.godkjenninghåndtererProvider(),
            personhåndterer = avhengigheter.personhåndtererProvider(),
            dokumenthåndterer = avhengigheter.dokumenthåndtererProvider(),
            stansAutomatiskBehandlinghåndterer = avhengigheter.stansAutomatiskBehandlinghåndterer(),
            vedtakBegrunnelseDao = daos.vedtakBegrunnelseDao,
        )

        routing {
            webSocketsApi()
            debugMinneApi()
        }
    }
}
