package no.nav.helse.spesialist.bootstrap

import com.github.navikt.tbd_libs.kafka.Config
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.github.navikt.tbd_libs.rapids_and_rivers.KafkaRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.FeatureToggles
import no.nav.helse.Gruppekontroll
import no.nav.helse.rapids_rivers.RapidApplication.Builder
import no.nav.helse.spesialist.api.AzureConfig
import no.nav.helse.spesialist.api.bootstrap.Gruppe
import no.nav.helse.spesialist.api.bootstrap.Tilgangsgrupper
import no.nav.helse.spesialist.application.Reservasjonshenter
import no.nav.helse.spesialist.application.Snapshothenter
import no.nav.helse.spesialist.db.testfixtures.DBTestFixture
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Instant.now
import java.util.Properties
import java.util.UUID

fun main() {
    LocalApp.start()
}

object LocalApp {
    private val mockOAuth2Server = MockOAuth2Server().also(MockOAuth2Server::start)
    private val clientId = "en-client-id"
    private val issuerId = "LocalTestIssuer"
    private val token =
        mockOAuth2Server.issueToken(
            issuerId = issuerId,
            audience = clientId,
            claims =
                mapOf(
                    "preferred_username" to "saksbehandler@nav.no",
                    "oid" to "${UUID.randomUUID()}",
                    "name" to "En Saksbehandler",
                    "NAVident" to "X123456",
                ),
        ).serialize().also {
            println("OAuth2-token:")
            println(it)
        }

    private val azureConfig =
        AzureConfig(
            clientId = clientId,
            issuerUrl = mockOAuth2Server.issuerUrl(issuerId).toString(),
            jwkProviderUri = mockOAuth2Server.jwksUrl(issuerId).toString(),
            tokenEndpoint = mockOAuth2Server.tokenEndpointUrl(issuerId).toString(),
        )

    private const val rapidTopic = "tbd.rapid.v1"
    val kafka = ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1")).apply {
        withReuse(true)
        start()
    }

    private val spesialistApp =
        SpesialistApp(
            env = EnvironmentImpl(mapOf("LOKAL_UTVIKLING" to "true")),
            gruppekontroll = gruppekontroll,
            snapshothenter = snapshothenter,
            azureConfig = azureConfig,
            tilgangsgrupper = tilgangsgrupper,
            reservasjonshenter = reservasjonshenter,
            versjonAvKode = "versjon_1",
            featureToggles = object : FeatureToggles {},
            dbModuleConfiguration = DBTestFixture.database.dbModuleConfiguration
        )

    private val localModule: Application.() -> Unit  = {
        routing {
            get("/local-token") {
                return@get call.respond(message = token)
            }
            playground()
        }
    }

    fun start() {
        spesialistApp.start(lagRapidsConnection())
    }

    private fun lagRapidsConnection(): RapidsConnection {
        val kafkaConfig = LocalKafkaConfig(
            mapOf(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers).toProperties()
        )

        val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        val kafkaRapid =
            KafkaRapid(ConsumerProducerFactory(kafkaConfig), "localApp-groupId", rapidTopic, meterRegistry)
        return Builder(
            appName = "spesialist-local",
            instanceId = "spesialist-instance-${now().epochSecond % 100}",
            rapid = kafkaRapid,
            meterRegistry = meterRegistry,
        ).apply {
            withKtorModule(spesialistApp::konfigurerKtorApp)
            withKtorModule(localModule)
        }.build()
    }
}

private val snapshothenter =
    object : Snapshothenter {
        override fun hentPerson(fødselsnummer: String) = null
    }

private val reservasjonshenter = Reservasjonshenter { null }

private val tilgangsgrupper =
    object : Tilgangsgrupper {
        override val kode7GruppeId: UUID = UUID.randomUUID()
        override val beslutterGruppeId: UUID = UUID.randomUUID()
        override val skjermedePersonerGruppeId: UUID = UUID.randomUUID()
        override val stikkprøveGruppeId: UUID = UUID.randomUUID()

        override fun gruppeId(gruppe: Gruppe): UUID {
            return when (gruppe) {
                Gruppe.KODE7 -> kode7GruppeId
                Gruppe.BESLUTTER -> beslutterGruppeId
                Gruppe.SKJERMEDE -> skjermedePersonerGruppeId
                Gruppe.STIKKPRØVE -> stikkprøveGruppeId
            }
        }
    }

private val gruppekontroll = object : Gruppekontroll {
    override suspend fun erIGrupper(oid: UUID, gruppeIder: List<UUID>) = true
}

private fun Route.playground() {
    get("playground") {
        call.respondText(buildPlaygroundHtml(), ContentType.Text.Html)
    }
}

private fun buildPlaygroundHtml() = Application::class.java.classLoader
    .getResource("graphql-playground.html")
    ?.readText()
    ?.replace("\${graphQLEndpoint}", "graphql")
    ?.replace("\${subscriptionsEndpoint}", "subscriptions")
    ?: throw IllegalStateException("graphql-playground.html cannot be found in the classpath")

// Kopiert fra tbd-libs:
private class LocalKafkaConfig(private val connectionProperties: Properties) : Config {
    override fun producerConfig(properties: Properties) = properties.apply {
        putAll(connectionProperties)
        put(ProducerConfig.ACKS_CONFIG, "all")
        put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
        put(ProducerConfig.LINGER_MS_CONFIG, "0")
        put(ProducerConfig.RETRIES_CONFIG, "0")
    }

    override fun consumerConfig(groupId: String, properties: Properties) = properties.apply {
        putAll(connectionProperties)
        put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    }

    override fun adminConfig(properties: Properties) = properties.apply {
        putAll(connectionProperties)
    }
}
