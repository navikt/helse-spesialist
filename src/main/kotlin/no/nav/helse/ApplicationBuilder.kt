package no.nav.helse

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.mediator.*
import no.nav.helse.mediator.api.*
import no.nav.helse.modell.*
import no.nav.helse.modell.abonnement.OpptegnelseDao
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
import no.nav.helse.modell.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.automatisering.AutomatiseringDao
import no.nav.helse.modell.automatisering.PlukkTilManuell
import no.nav.helse.modell.dkif.DigitalKontaktinformasjonDao
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDao
import no.nav.helse.modell.leggpåvent.LeggPåVentMediator
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.saksbehandler.SaksbehandlerDao
import no.nav.helse.modell.tildeling.ReservasjonDao
import no.nav.helse.modell.tildeling.TildelingDao
import no.nav.helse.modell.tildeling.TildelingMediator
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.net.ProxySelector
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.random.Random.Default.nextInt

const val azureMountPath: String = "/var/run/secrets/nais.io/azure"
private val auditLog = LoggerFactory.getLogger("auditLogger")
private val logg = LoggerFactory.getLogger("ApplicationBuilder")
private val sikkerLog = LoggerFactory.getLogger("tjenestekall")

internal class ApplicationBuilder(env: Map<String, String>) : RapidsConnection.StatusListener {
    private val dataSourceBuilder = DataSourceBuilder(System.getenv())
    private val dataSource = dataSourceBuilder.getDataSource(DataSourceBuilder.Role.User)

    private val azureAdClient = HttpClient(Apache) {
        engine {
            customizeClient {
                setRoutePlanner(SystemDefaultRoutePlanner(ProxySelector.getDefault()))
            }
        }
        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerModule(JavaTimeModule())
            }
        }
    }
    private val spleisClient = HttpClient {
        install(JsonFeature) { serializer = JacksonSerializer() }
    }
    private val oidcDiscovery =
        runBlocking { AzureAadClient(azureAdClient).oidcDiscovery(System.getenv("AZURE_CONFIG_URL")) }
    private val accessTokenClient = AccessTokenClient(
        aadAccessTokenUrl = oidcDiscovery.token_endpoint,
        clientId = readClientId(),
        clientSecret = readClientSecret(),
        httpClient = azureAdClient
    )
    private val speilSnapshotRestClient = SpeilSnapshotRestClient(
        httpClient = spleisClient,
        accessTokenClient = accessTokenClient,
        spleisClientId = env.getValue("SPLEIS_CLIENT_ID")
    )

    private val azureConfig = AzureAdAppConfig(
        clientId = readClientId(),
        speilClientId = env.getValue("SPEIL_CLIENT_ID"),
        requiredGroup = env.getValue("AZURE_REQUIRED_GROUP")
    )
    private val httpTraceLog = LoggerFactory.getLogger("tjenestekall")
    private lateinit var hendelseMediator: HendelseMediator

    private val personDao = PersonDao(dataSource)
    private val oppgaveDao = OppgaveDao(dataSource)
    private val vedtakDao = VedtakDao(dataSource)
    private val warningDao = WarningDao(dataSource)
    private val risikovurderingDao = RisikovurderingDao(dataSource)
    private val saksbehandlerDao = SaksbehandlerDao(dataSource)
    private val commandContextDao = CommandContextDao(dataSource)
    private val tildelingDao = TildelingDao(dataSource)
    private val digitalKontaktinformasjonDao = DigitalKontaktinformasjonDao(dataSource)
    private val åpneGosysOppgaverDao = ÅpneGosysOppgaverDao(dataSource)
    private val overstyringDao = OverstyringDao(dataSource)
    private val reservasjonDao = ReservasjonDao(dataSource)
    private val snapshotDao = SnapshotDao(dataSource)
    private val arbeidsgiverDao = ArbeidsgiverDao(dataSource)
    private val hendelseDao = HendelseDao(dataSource)
    private val egenAnsattDao = EgenAnsattDao(dataSource)
    private val utbetalingDao = UtbetalingDao(dataSource)
    private val arbeidsforholdDao = ArbeidsforholdDao(dataSource)
    private val opptegnelseDao = OpptegnelseDao(dataSource)

    private val oppgaveMediator = OppgaveMediator(
        oppgaveDao,
        vedtakDao,
        tildelingDao,
        reservasjonDao
    )

    private val plukkTilManuell: PlukkTilManuell = if (Toggles.Stikkprøver.enabled) {
        val divisor =
            requireNotNull(env["STIKKPROEVER_DIVISOR"]) { "STIKKPROEVER_DIVISOR må oppgis når stikkprøver er aktivert" }.toInt()
        require(divisor > 0) { "Her er et vennlig tips: ikke prøv å dele på 0" }
        ({ nextInt(divisor) == 0 })
    } else {
        { false }
    }

    private val hendelsefabrikk = Hendelsefabrikk(
        hendelseDao = hendelseDao,
        personDao = personDao,
        arbeidsgiverDao = arbeidsgiverDao,
        vedtakDao = vedtakDao,
        warningDao = warningDao,
        commandContextDao = commandContextDao,
        snapshotDao = snapshotDao,
        oppgaveDao = oppgaveDao,
        reservasjonDao = reservasjonDao,
        saksbehandlerDao = saksbehandlerDao,
        overstyringDao = overstyringDao,
        risikovurderingDao = risikovurderingDao,
        digitalKontaktinformasjonDao = digitalKontaktinformasjonDao,
        åpneGosysOppgaverDao = åpneGosysOppgaverDao,
        egenAnsattDao = egenAnsattDao,
        arbeidsforholdDao = arbeidsforholdDao,
        speilSnapshotRestClient = speilSnapshotRestClient,
        oppgaveMediator = oppgaveMediator,
        godkjenningMediator = GodkjenningMediator(warningDao, vedtakDao),
        automatisering = Automatisering(
            warningDao = warningDao,
            risikovurderingDao = risikovurderingDao,
            automatiseringDao = AutomatiseringDao(dataSource),
            digitalKontaktinformasjonDao = digitalKontaktinformasjonDao,
            åpneGosysOppgaverDao = åpneGosysOppgaverDao,
            egenAnsattDao = egenAnsattDao,
            personDao = personDao,
            vedtakDao = vedtakDao,
            plukkTilManuell = plukkTilManuell
        ),
        utbetalingDao = utbetalingDao,
        opptegnelseDao = opptegnelseDao
    )

    private val rapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env)).withKtorModule {
            install(CORS) {
                header(HttpHeaders.AccessControlAllowOrigin)
                host("speil.nais.adeo.no", listOf("https"))
            }
            install(CallId) {
                generate {
                    UUID.randomUUID().toString()
                }
            }
            installErrorHandling()
            install(CallLogging) {
                logger = httpTraceLog
                level = Level.INFO
                callIdMdc("callId")
                filter { call -> call.request.path().startsWith("/api/") }
            }
            intercept(ApplicationCallPipeline.Call) {
                call.principal<JWTPrincipal>()?.let { principal ->
                    auditLog.info(
                        "Bruker=\"${
                            principal.payload.getClaim("NAVident")
                                .asString()
                        }\" gjør kall mot url=\"${call.request.uri}\""
                    )
                }
            }
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            requestResponseTracing(httpTraceLog)
            azureAdAppAuthentication(
                oidcDiscovery = oidcDiscovery,
                config = azureConfig
            )
            basicAuthentication(env.getValue("ADMIN_SECRET"))
            routing {
                authenticate("saksbehandler") {
                    oppgaveApi(oppgaveMediator, env.getValue("RISK_SUPERSAKSBEHANDLER_GROUP"))
                    vedtaksperiodeApi(
                        hendelseMediator = hendelseMediator,
                        vedtaksperiodeMediator = VedtaksperiodeMediator(
                            vedtakDao = vedtakDao,
                            warningDao = warningDao,
                            personDao = personDao,
                            arbeidsgiverDao = arbeidsgiverDao,
                            snapshotDao = snapshotDao,
                            overstyringDao = overstyringDao,
                            oppgaveDao = oppgaveDao,
                            tildelingDao = tildelingDao,
                            risikovurderingDao = risikovurderingDao,
                            utbetalingDao = utbetalingDao,
                            arbeidsforholdDao = arbeidsforholdDao
                        )
                    )
                    tildelingApi(TildelingMediator(saksbehandlerDao, tildelingDao, hendelseMediator))
                    annulleringApi(hendelseMediator)
                    opptegnelseApi(OpptegnelseMediator(opptegnelseDao))
                    leggPåVentApi(LeggPåVentMediator(tildelingDao, oppgaveDao, hendelseMediator))
                }
                authenticate("saksbehandler-direkte") {
                    direkteOppgaveApi(oppgaveMediator)
                }
            }
            adminApi(hendelseMediator)
        }.build()

    init {
        rapidsConnection.register(this)
        hendelseMediator = HendelseMediator(
            rapidsConnection = rapidsConnection,
            oppgaveDao = oppgaveDao,
            personDao = personDao,
            vedtakDao = vedtakDao,
            commandContextDao = commandContextDao,
            hendelseDao = hendelseDao,
            reservasjonDao = reservasjonDao,
            tildelingDao = tildelingDao,
            hendelsefabrikk = hendelsefabrikk,
            oppgaveMediator = oppgaveMediator
        )
    }

    fun start() = rapidsConnection.start()
    fun stop() = rapidsConnection.stop()

    override fun onShutdown(rapidsConnection: RapidsConnection) {
        hendelseMediator.shutdown()
    }

    override fun onStartup(rapidsConnection: RapidsConnection) {
        dataSourceBuilder.migrate()
    }

    private fun readClientId(): String {
        return Files.readString(Paths.get(azureMountPath, "client_id"))
    }

    private fun readClientSecret(): String {
        return Files.readString(Paths.get(azureMountPath, "client_secret"))
    }
}

fun Application.installErrorHandling() {
    install(StatusPages) {
        exception<Throwable> { cause ->
            val uri = call.request.uri
            val verb = call.request.httpMethod.value
            logg.error("Unhandled: $verb", cause)
            sikkerLog.error("Unhandled: $verb - $uri", cause)
            call.respond(HttpStatusCode.InternalServerError, "Det skjedde en uventet feil")
        }
    }
}
