package no.nav.helse

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.principal
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.CallId
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.callIdMdc
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.request.path
import io.ktor.request.uri
import kotlinx.coroutines.runBlocking
import no.nav.helse.api.oppgaveApi
import no.nav.helse.api.vedtaksperiodeApi
import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.mediator.kafka.meldinger.ArbeidsgiverMessage
import no.nav.helse.mediator.kafka.meldinger.GodkjenningMessage
import no.nav.helse.mediator.kafka.meldinger.PersoninfoLøsningMessage
import no.nav.helse.mediator.kafka.meldinger.PåminnelseMessage
import no.nav.helse.modell.dao.ArbeidsgiverDao
import no.nav.helse.modell.dao.OppgaveDao
import no.nav.helse.modell.dao.PersonDao
import no.nav.helse.modell.dao.SnapshotDao
import no.nav.helse.modell.dao.SpeilSnapshotRestDao
import no.nav.helse.modell.dao.SpleisbehovDao
import no.nav.helse.modell.dao.VedtakDao
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.net.ProxySelector
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

const val azureMountPath: String = "/var/run/secrets/nais.io/azure"
private val auditLog = LoggerFactory.getLogger("auditLogger")

internal class ApplicationBuilder(env: Map<String, String>) : RapidsConnection.StatusListener {
    private val dataSourceBuilder = DataSourceBuilder(System.getenv())
    private val dataSource = dataSourceBuilder.getDataSource(DataSourceBuilder.Role.User)

    private val personDao = PersonDao(dataSource)
    private val arbeidsgiverDao = ArbeidsgiverDao(dataSource)
    private val vedtakDao = VedtakDao(dataSource)
    private val spleisbehovDao = SpleisbehovDao(dataSource)
    private val snapshotDao = SnapshotDao(dataSource)
    private val oppgaveDao = OppgaveDao(dataSource)

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
    private val speilSnapshotRestDao = SpeilSnapshotRestDao(
        httpClient = spleisClient,
        accessTokenClient = accessTokenClient,
        spleisClientId = System.getenv("SPLEIS_CLIENT_ID")
    )

    private val azureConfig = AzureAdAppConfig(
        clientId = System.getenv("SPEIL_CLIENT_ID"),//readClientId(),
        requiredGroup = env.getValue("AZURE_REQUIRED_GROUP")
    )
    private val httpTraceLog = LoggerFactory.getLogger("tjenestekall")
    private val spleisbehovMediator = SpleisbehovMediator(
        spleisbehovDao = spleisbehovDao,
        personDao = personDao,
        arbeidsgiverDao = arbeidsgiverDao,
        vedtakDao = vedtakDao,
        snapshotDao = snapshotDao,
        speilSnapshotRestDao = speilSnapshotRestDao,
        oppgaveDao = oppgaveDao
    )
    private val rapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env)).withKtorModule {
            install(CallId) {
                generate {
                    UUID.randomUUID().toString()
                }
            }
            install(CallLogging) {
                logger = httpTraceLog
                level = Level.INFO
                callIdMdc("callId")
                filter { call -> call.request.path().startsWith("/api/") }
            }
            intercept(ApplicationCallPipeline.Call) {
                call.principal<JWTPrincipal>()?.let { principal ->
                    auditLog.info(
                        "Bruker=\"${principal.payload.getClaim("NAVident")
                            .asString()}\" gjør kall mot url=\"${call.request.uri}\""
                    )
                }
            }
            install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
            requestResponseTracing(httpTraceLog)
            azureAdAppAuthentication(oidcDiscovery, azureConfig)
            oppgaveApi(oppgaveDao = oppgaveDao, personDao = personDao, vedtakDao = vedtakDao)
            vedtaksperiodeApi(
                personDao = personDao,
                vedtakDao = vedtakDao,
                snapshotDao = snapshotDao,
                arbeidsgiverDao = arbeidsgiverDao,
                spleisbehovMediator = spleisbehovMediator
            )
        }.build()

    init {
        spleisbehovMediator.init(rapidsConnection)
        rapidsConnection.register(this)

        ArbeidsgiverMessage.Factory(rapidsConnection, spleisbehovMediator)
        GodkjenningMessage.Factory(rapidsConnection, spleisbehovMediator)
        PersoninfoLøsningMessage.Factory(rapidsConnection, spleisbehovMediator)
        PåminnelseMessage.Factory(rapidsConnection, spleisbehovMediator)
    }

    fun start() = rapidsConnection.start()
    fun stop() = rapidsConnection.stop()

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
