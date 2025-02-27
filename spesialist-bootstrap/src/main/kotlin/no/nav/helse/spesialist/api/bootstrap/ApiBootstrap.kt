package no.nav.helse.spesialist.api.bootstrap

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
import no.nav.helse.spesialist.api.graphql.graphQLApi
import no.nav.helse.spesialist.api.snapshot.SnapshotService
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
    ): Application =
        application.apply {
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
                snapshotService = SnapshotService(daos.personinfoDao, snapshothenter),
                behandlingsstatistikkMediator = behandlingstatistikk,
                saksbehandlerMediator = saksbehandlerMediator,
                apiOppgaveService = apiOppgaveService,
                godkjenninghåndterer = godkjenninghåndterer,
                personhåndterer = personhåndterer,
                dokumenthåndterer = dokumenthåndterer,
                stansAutomatiskBehandlinghåndterer = stansAutomatiskBehandlinghåndterer,
                vedtakBegrunnelseDao = daos.vedtakBegrunnelseDao,
            )

            routing {
                webSocketsApi()
                debugMinneApi()
            }
        }
}
