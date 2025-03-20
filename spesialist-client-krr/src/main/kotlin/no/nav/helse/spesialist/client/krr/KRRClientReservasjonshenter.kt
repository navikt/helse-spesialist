package no.nav.helse.spesialist.client.krr

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.Reservasjonshenter
import no.nav.helse.spesialist.application.Reservasjonshenter.ReservasjonDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

private val registry = Metrics.globalRegistry.add(PrometheusMeterRegistry(PrometheusConfig.DEFAULT))

private val responstidReservasjonsstatus =
    Timer
        .builder("responstid_hent_reservasjonsstatus")
        .description("Responstid for kall til digdir-krr-proxy")
        .register(registry)

private val statusEtterKallReservasjonsstatusBuilder =
    Counter
        .builder("status_kall_hent_reservasjonsstatus")
        .description("Status på kall til digdir-krr-proxy, success eller failure")
        .tags(listOf(Tag.of("status", "success"), Tag.of("status", "failure")))

class KRRClientReservasjonshenter(
    private val configuration: Configuration.Client,
    private val accessTokenGenerator: AccessTokenGenerator,
) : Reservasjonshenter {
    data class Configuration(
        val client: Client?,
    ) {
        data class Client(
            val apiUrl: String,
            val scope: String,
        )
    }

    private val logg: Logger = LoggerFactory.getLogger(this.javaClass)
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
    private val httpClient: HttpClient =
        HttpClient(Apache) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter())
            }
            engine {
                socketTimeout = 5_000
                connectTimeout = 5_000
                connectionRequestTimeout = 5_000
            }
        }

    override suspend fun hentForPerson(fødselsnummer: String): ReservasjonDto? {
        val sample = Timer.start(registry)
        return try {
            logg.debug("Henter accessToken")
            val accessToken = accessTokenGenerator.hentAccessToken(configuration.scope)
            val callId = UUID.randomUUID().toString()

            logg.debug("Henter reservasjon fra ${configuration.apiUrl}/rest/v1/person, callId=$callId")
            val response =
                httpClient
                    .get("${configuration.apiUrl}/rest/v1/person") {
                        header("Authorization", "Bearer $accessToken")
                        header("Nav-Personident", fødselsnummer)
                        header("Nav-Call-Id", callId)
                        accept(ContentType.Application.Json)
                    }.body<JsonNode>()

            statusEtterKallReservasjonsstatusBuilder
                .withRegistry(registry)
                .withTag("status", "success")

            ReservasjonDto(
                kanVarsles = response.getBoolean("kanVarsles"),
                reservert = response.getBoolean("reservert"),
            )
        } catch (e: Exception) {
            statusEtterKallReservasjonsstatusBuilder
                .withRegistry(registry)
                .withTag("status", "failure")
            logg.warn("Feil under kall til Kontakt- og reservasjonsregisteret")
            sikkerLogg.warn("Feil under kall til Kontakt- og reservasjonsregisteret", e)
            null
        } finally {
            sample.stop(responstidReservasjonsstatus)
        }
    }

    private fun JsonNode.getBoolean(fieldName: String) =
        this[fieldName].let { fieldNode ->
            fieldNode.takeIf(JsonNode::isBoolean)?.asBoolean() ?: error("Fikk ugyldig boolean-verdi: $fieldNode")
        }
}
