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
    daos: Daos,
    private val sessionFactory: SessionFactory,
    private val avhengigheter: ApiAvhengigheter,
    private val reservasjonshenter: Reservasjonshenter,
    private val tilgangsgrupper: Tilgangsgrupper,
) {
    private val personinfoDao = daos.personinfoDao
    private val personApiDao = daos.personApiDao
    private val oppgaveApiDao = daos.oppgaveApiDao
    private val periodehistorikkApiDao = daos.periodehistorikkApiDao
    private val risikovurderingApiDao = daos.risikovurderingApiDao
    private val tildelingApiDao = daos.tildelingApiDao
    private val overstyringApiDao = daos.overstyringApiDao
    private val arbeidsgiverApiDao = daos.arbeidsgiverApiDao
    private val egenAnsattApiDao = daos.egenAnsattApiDao
    private val notatApiDao = daos.notatApiDao
    private val totrinnsvurderingApiDao = daos.totrinnsvurderingApiDao
    private val apiVarselRepository = daos.varselApiRepository
    private val påVentApiDao = daos.påVentApiDao
    private val vergemålApiDao = daos.vergemålApiDao

    fun ktorApp(
        application: Application,
        azureConfig: AzureConfig,
        env: Environment,
    ) = application.apply {
        installPlugins()
        azureAdAppAuthentication(azureConfig, env)
        graphQLApi(
            sessionFactory = sessionFactory,
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
            reservasjonshenter = reservasjonshenter,
            skjermedePersonerGruppeId = tilgangsgrupper.skjermedePersonerGruppeId,
            kode7Saksbehandlergruppe = tilgangsgrupper.kode7GruppeId,
            beslutterGruppeId = tilgangsgrupper.beslutterGruppeId,
            snapshotService = SnapshotService(personinfoDao, avhengigheter.snapshothenter),
            behandlingsstatistikkMediator = avhengigheter.behandlingstatistikk,
            saksbehandlerMediator = avhengigheter.saksbehandlerMediatorProvider(),
            apiOppgaveService = avhengigheter.apiOppgaveServiceProvider(),
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
