package no.nav.helse.spesialist.api.bootstrap

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import no.nav.helse.bootstrap.Environment
import no.nav.helse.spesialist.api.AzureConfig
import no.nav.helse.spesialist.api.arbeidsgiver.ArbeidsgiverApiDao
import no.nav.helse.spesialist.api.azureAdAppAuthentication
import no.nav.helse.spesialist.api.egenAnsatt.EgenAnsattApiDao
import no.nav.helse.spesialist.api.graphql.graphQLApi
import no.nav.helse.spesialist.api.notat.NotatApiDao
import no.nav.helse.spesialist.api.oppgave.OppgaveApiDao
import no.nav.helse.spesialist.api.overstyring.OverstyringApiDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkApiDao
import no.nav.helse.spesialist.api.person.PersonApiDao
import no.nav.helse.spesialist.api.påvent.PåVentApiDao
import no.nav.helse.spesialist.api.reservasjon.ReservasjonClient
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDao
import no.nav.helse.spesialist.api.snapshot.SnapshotApiDao
import no.nav.helse.spesialist.api.snapshot.SnapshotService
import no.nav.helse.spesialist.api.tildeling.TildelingApiDao
import no.nav.helse.spesialist.api.totrinnsvurdering.TotrinnsvurderingApiDao
import no.nav.helse.spesialist.api.varsel.ApiVarselRepository
import no.nav.helse.spesialist.api.vergemål.VergemålApiDao
import no.nav.helse.spesialist.api.websockets.webSocketsApi
import javax.sql.DataSource

class Bootstrap(
    dataSource: DataSource,
    private val avhengigheter: ApiAvhengigheter,
    private val reservasjonClient: ReservasjonClient,
    private val tilgangsgrupper: Tilgangsgrupper,
) {
    private val snapshotApiDao = SnapshotApiDao(dataSource)
    private val personApiDao = PersonApiDao(dataSource)
    private val oppgaveApiDao = OppgaveApiDao(dataSource)
    private val periodehistorikkApiDao = PeriodehistorikkApiDao(dataSource)
    private val risikovurderingApiDao = RisikovurderingApiDao(dataSource)
    private val tildelingApiDao = TildelingApiDao(dataSource)
    private val overstyringApiDao = OverstyringApiDao(dataSource)
    private val arbeidsgiverApiDao = ArbeidsgiverApiDao(dataSource)
    private val egenAnsattApiDao = EgenAnsattApiDao(dataSource)
    private val notatApiDao = NotatApiDao(dataSource)
    private val totrinnsvurderingApiDao = TotrinnsvurderingApiDao(dataSource)
    private val apiVarselRepository = ApiVarselRepository(dataSource)
    private val påVentApiDao = PåVentApiDao(dataSource)
    private val vergemålApiDao = VergemålApiDao(dataSource)

    fun ktorApp(
        azureConfig: AzureConfig,
        env: Environment,
    ): Application.() -> Unit =
        {
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
