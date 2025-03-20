package no.nav.helse.spesialist.testfixtures

import com.github.navikt.tbd_libs.kafka.Config
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.github.navikt.tbd_libs.rapids_and_rivers.KafkaRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.server.application.Application
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.spesialist.kafka.KafkaModule
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.util.Properties

object KafkaModuleIntegrationTestFixture {
    private val kafka = ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1")).apply {
        withReuse(true)
        start()
    }

    private val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    private val kafkaRapid =
        KafkaRapid(
            factory = ConsumerProducerFactory(
                LocalKafkaConfig(
                    mapOf(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers).toProperties()
                )
            ),
            groupId = "localApp-groupId",
            rapidTopic = "tbd.rapid.v1",
            meterRegistry = meterRegistry
        )

    fun createRapidApplication(ktorModule: (Application) -> Unit): RapidsConnection = RapidApplication.Builder(
        appName = "spesialist-local",
        instanceId = "spesialist-instance-${Instant.now().epochSecond % 100}",
        rapid = kafkaRapid,
        meterRegistry = meterRegistry,
    ).apply {
        withKtorModule(ktorModule)
    }.build()

    private class LocalKafkaConfig(private val connectionProperties: Properties) : Config {
        // Kopiert fra tbd-libs
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

    val moduleConfiguration = KafkaModule.Configuration(
        versjonAvKode = "versjon_1",
        ignorerMeldingerForUkjentePersoner = false,
    )
}