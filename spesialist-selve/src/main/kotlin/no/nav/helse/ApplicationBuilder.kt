package no.nav.helse

import com.auth0.jwk.JwkProviderBuilder
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
import no.nav.helse.arbeidsgiver.ArbeidsgiverDao
import no.nav.helse.behandlingsstatistikk.BehandlingsstatistikkDao
import no.nav.helse.behandlingsstatistikk.BehandlingsstatistikkMediator
import no.nav.helse.behandlingsstatistikk.behandlingsstatistikkApi
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.Hendelsefabrikk
import no.nav.helse.mediator.OpptegnelseMediator
import no.nav.helse.mediator.api.*
import no.nav.helse.modell.*
import no.nav.helse.modell.abonnement.OpptegnelseDao
import no.nav.helse.modell.arbeidsforhold.ArbeidsforholdDao
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
import no.nav.helse.modell.tildeling.TildelingMediator
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.oppgave.OppgaveDao
import no.nav.helse.oppgave.OppgaveMediator
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.reservasjon.ReservasjonDao
import no.nav.helse.saksbehandler.SaksbehandlerDao
import no.nav.helse.tildeling.TildelingDao
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.net.ProxySelector
import java.net.URL
import java.util.*
import kotlin.random.Random.Default.nextInt

const val azureMountPath: String = "/var/run/secrets/nais.io/azure"
private val auditLog = LoggerFactory.getLogger("auditLogger")
private val logg = LoggerFactory.getLogger("ApplicationBuilder")
private val sikkerLog = LoggerFactory.getLogger("tjenestekall")

internal class ApplicationBuilder(env: Map<String, String>) : RapidsConnection.StatusListener {
    private val dataSourceBuilder = DataSourceBuilder(System.getenv())
    private val dataSource = dataSourceBuilder.getDataSource()

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
    private val spleisClient = HttpClient(Apache) {
        install(JsonFeature) { serializer = JacksonSerializer() }
        engine {
            socketTimeout = 30_000
            connectTimeout = 30_000
            connectionRequestTimeout = 40_000
        }
    }
    private val accessTokenClient = AccessTokenClient(
        aadAccessTokenUrl = env.getValue("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
        clientId = env.getValue("AZURE_APP_CLIENT_ID"),
        clientSecret = env.getValue("AZURE_APP_CLIENT_SECRET"),
        httpClient = azureAdClient
    )
    private val speilSnapshotRestClient = SpeilSnapshotRestClient(
        httpClient = spleisClient,
        accessTokenClient = accessTokenClient,
        spleisClientId = env.getValue("SPLEIS_CLIENT_ID")
    )

    private val azureConfig = AzureAdAppConfig(
        clientId = env.getValue("AZURE_APP_CLIENT_ID"),
        issuer = env.getValue("AZURE_OPENID_CONFIG_ISSUER"),
        jwkProvider = JwkProviderBuilder(URL(env.getValue("AZURE_OPENID_CONFIG_JWKS_URI"))).build()
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
    private val behandlingsstatistikkDao = BehandlingsstatistikkDao(dataSource)

    private val oppgaveMediator = OppgaveMediator(
        oppgaveDao,
        tildelingDao,
        reservasjonDao
    )

    private val plukkTilManuell: PlukkTilManuell = ({
        env["STIKKPROEVER_DIVISOR"]?.let {
            val divisor = it.toInt()
            require(divisor > 0) { "Her er et vennlig tips: ikke prøv å dele på 0" }
            nextInt(divisor) == 0
        } ?: false
    })


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
        tildelingDao = tildelingDao,
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
            azureAdAppAuthentication(azureConfig)
            basicAuthentication(env.getValue("ADMIN_SECRET"))
            routing {
                authenticate("oidc") {
                    oppgaveApi(oppgaveMediator, env.getValue("RISK_SUPERSAKSBEHANDLER_GROUP"))
                    vedtaksperiodeApi(
                        hendelseMediator = hendelseMediator,
                        vedtaksperiodeMediator = VedtaksperiodeMediator(
                            vedtakDao = vedtakDao,
                            warningDao = warningDao,
                            personDao = personDao,
                            arbeidsgiverDao = arbeidsgiverDao,
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
                    leggPåVentApi(LeggPåVentMediator(tildelingDao, hendelseMediator))
                    behandlingsstatistikkApi(BehandlingsstatistikkMediator(behandlingsstatistikkDao))
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
            saksbehandlerDao = saksbehandlerDao,
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
