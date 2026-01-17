package no.nav.helse.spesialist.client.krr

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
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
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.sikkerlogg
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
    private val configuration: ClientKrrModule.Configuration.Client,
    private val accessTokenGenerator: AccessTokenGenerator,
) : Reservasjonshenter {
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
                    .post("${configuration.apiUrl}/rest/v1/personer") {
                        header("Authorization", "Bearer $accessToken")
                        header("Nav-Call-Id", callId)
                        accept(ContentType.Application.Json)
                        contentType(ContentType.Application.Json)
                        setBody(""" { "personidenter": [ "$fødselsnummer" ] } """)
                    }.body<JsonNode>()

            statusEtterKallReservasjonsstatusBuilder
                .withRegistry(registry)
                .withTag("status", "success")
            parseResponse(response, fødselsnummer)
        } catch (e: Exception) {
            statusEtterKallReservasjonsstatusBuilder
                .withRegistry(registry)
                .withTag("status", "failure")
            logg.warn("Feil under kall til Kontakt- og reservasjonsregisteret")
            sikkerlogg.warn("Feil under kall til Kontakt- og reservasjonsregisteret", e)
            null
        } finally {
            sample.stop(responstidReservasjonsstatus)
        }
    }

    private fun parseResponse(
        response: JsonNode,
        fødselsnummer: String,
    ): ReservasjonDto? {
        val feil = response["feil"]
        return if (!feil.isEmpty) {
            logg.warn("Feil fra Kontakt- og reservasjonsregisteret")
            sikkerlogg.warn("Feil fra Kontakt- og reservasjonsregisteret: {}", feil)
            null
        } else {
            response["personer"][fødselsnummer].let {
                ReservasjonDto(
                    kanVarsles = it.getBoolean("kanVarsles"),
                    reservert = it.getBoolean("reservert"),
                )
            }
        }
    }

    private fun JsonNode.getBoolean(fieldName: String) =
        this[fieldName].let { fieldNode ->
            fieldNode.takeIf(JsonNode::isBoolean)?.asBoolean() ?: error("Fikk ugyldig boolean-verdi: $fieldNode")
        }
}
