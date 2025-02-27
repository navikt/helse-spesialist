package no.nav.helse.spesialist.bootstrap

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import no.nav.helse.bootstrap.Environment
import no.nav.helse.db.Daos
import no.nav.helse.db.SessionFactory
import no.nav.helse.mediator.SaksbehandlerMediator
import no.nav.helse.mediator.oppgave.ApiOppgaveService
import no.nav.helse.spesialist.api.AzureConfig
import no.nav.helse.spesialist.api.Dokumenthåndterer
import no.nav.helse.spesialist.api.Godkjenninghåndterer
import no.nav.helse.spesialist.api.Personhåndterer
import no.nav.helse.spesialist.api.StansAutomatiskBehandlinghåndterer
import no.nav.helse.spesialist.api.azureAdAppAuthentication
import no.nav.helse.spesialist.api.behandlingsstatistikk.IBehandlingsstatistikkService
import no.nav.helse.spesialist.api.bootstrap.Tilgangsgrupper
import no.nav.helse.spesialist.api.bootstrap.debugMinneApi
import no.nav.helse.spesialist.api.bootstrap.installPlugins
import no.nav.helse.spesialist.api.graphql.settOppGraphQLApi
import no.nav.helse.spesialist.api.websockets.webSocketsApi
import no.nav.helse.spesialist.application.Reservasjonshenter
import no.nav.helse.spesialist.application.Snapshothenter

class ApiBootstrap(
    private val daos: Daos,
    private val sessionFactory: SessionFactory,
    private val saksbehandlerMediator: SaksbehandlerMediator,
    private val apiOppgaveService: ApiOppgaveService,
    private val godkjenninghåndterer: Godkjenninghåndterer,
    private val personhåndterer: Personhåndterer,
    private val dokumenthåndterer: Dokumenthåndterer,
    private val stansAutomatiskBehandlinghåndterer: StansAutomatiskBehandlinghåndterer,
    private val behandlingstatistikk: IBehandlingsstatistikkService,
    private val snapshothenter: Snapshothenter,
    private val reservasjonshenter: Reservasjonshenter,
    private val tilgangsgrupper: Tilgangsgrupper,
) {
    fun konfigurerKtorApp(
        application: Application,
        azureConfig: AzureConfig,
        env: Environment,
    ) {
        application.apply {
            installPlugins()
            azureAdAppAuthentication(azureConfig, env)
            settOppGraphQLApi(
                daos = daos,
                sessionFactory = sessionFactory,
                saksbehandlerMediator = saksbehandlerMediator,
                apiOppgaveService = apiOppgaveService,
                godkjenninghåndterer = godkjenninghåndterer,
                personhåndterer = personhåndterer,
                dokumenthåndterer = dokumenthåndterer,
                stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlinghåndterer,
                behandlingstatistikk = behandlingstatistikk,
                snapshothenter = snapshothenter,
                reservasjonshenter = reservasjonshenter,
                tilgangsgrupper = tilgangsgrupper,
            )

            routing {
                webSocketsApi()
                debugMinneApi()
            }
        }
    }
}
