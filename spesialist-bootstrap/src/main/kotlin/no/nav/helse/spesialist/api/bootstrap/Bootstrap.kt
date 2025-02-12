package no.nav.helse.spesialist.api.bootstrap

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import no.nav.helse.bootstrap.Environment
import no.nav.helse.db.Repositories
import no.nav.helse.db.SessionFactory
import no.nav.helse.spesialist.api.AzureConfig
import no.nav.helse.spesialist.api.azureAdAppAuthentication
import no.nav.helse.spesialist.api.graphql.graphQLApi
import no.nav.helse.spesialist.api.snapshot.SnapshotService
import no.nav.helse.spesialist.api.websockets.webSocketsApi
import no.nav.helse.spesialist.application.Reservasjonshenter

class Bootstrap(
    repositories: Repositories,
    private val sessionFactory: SessionFactory,
    private val avhengigheter: ApiAvhengigheter,
    private val reservasjonshenter: Reservasjonshenter,
    private val tilgangsgrupper: Tilgangsgrupper,
) {
    private val personinfoDao = repositories.personinfoDao
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
    private val apiVarselRepository = repositories.varselApiRepository
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
            avviksvurderinghenter = avhengigheter.avviksvurderinghenter,
            skjermedePersonerGruppeId = tilgangsgrupper.skjermedePersonerGruppeId,
            kode7Saksbehandlergruppe = tilgangsgrupper.kode7GruppeId,
            beslutterGruppeId = tilgangsgrupper.beslutterGruppeId,
            snapshotService = SnapshotService(personinfoDao, avhengigheter.snapshotClient),
            behandlingsstatistikkMediator = avhengigheter.behandlingstatistikk,
            saksbehandlerhåndterer = avhengigheter.saksbehandlerhåndtererProvider(),
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
