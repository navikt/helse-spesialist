package no.nav.helse.spesialist.client.krr

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.KrrRegistrertStatusHenter
import no.nav.helse.spesialist.application.KrrRegistrertStatusHenter.KrrRegistrertStatus
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.loggErrorWithNoThrowable
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.util.Timeout
import java.time.Duration
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

class KRRClientKrrRegistrertStatusHenter(
    private val configuration: ClientKrrModule.Configuration.Client,
    private val accessTokenGenerator: AccessTokenGenerator,
) : KrrRegistrertStatusHenter {
    private val objectMapper = jacksonObjectMapper()

    override fun hentForPerson(fødselsnummer: String): KrrRegistrertStatus {
        val sample = Timer.start(registry)
        return try {
            val accessToken = accessTokenGenerator.hentAccessToken(configuration.scope)
            val callId = UUID.randomUUID().toString()

            logg.debug("Henter reservasjon fra ${configuration.apiUrl}/rest/v1/person, callId=$callId")
            Request
                .post("${configuration.apiUrl}/rest/v1/personer")
                .setHeader("Authorization", "Bearer $accessToken")
                .setHeader("Nav-Call-Id", callId)
                .setHeader("Accept", ContentType.APPLICATION_JSON.mimeType)
                .connectTimeout(Timeout.of(Duration.ofSeconds(5)))
                .responseTimeout(Timeout.of(Duration.ofSeconds(5)))
                .bodyString(""" { "personidenter": [ "$fødselsnummer" ] } """, ContentType.APPLICATION_JSON)
                .execute()
                .handleResponse { response ->
                    val responseBody = EntityUtils.toString(response.entity)
                    if (response.code !in 200..299) {
                        responseError("Fikk HTTP ${response.code} tilbake fra KRR", responseBody)
                    }

                    val responseJson = objectMapper.readTree(responseBody)

                    statusEtterKallReservasjonsstatusBuilder
                        .withRegistry(registry)
                        .withTag("status", "success")

                    if (responseJson["feil"]?.isEmpty == false) {
                        responseError("Fikk feil tilbake fra KRR", responseJson.toString())
                    } else {
                        responseJson["personer"][fødselsnummer].let {
                            if (it == null) {
                                responseError("Fant ikke igjen personen i responsen fra KRR", responseJson.toString())
                            }
                            if (it.getIfBoolean("aktiv") == false) {
                                KrrRegistrertStatus.IKKE_REGISTRERT_I_KRR
                            } else {
                                val reservert = it.getRequiredBoolean("reservert", responseJson)
                                val kanVarsles = it.getRequiredBoolean("kanVarsles", responseJson)
                                if (reservert || !kanVarsles) {
                                    KrrRegistrertStatus.RESERVERT_MOT_DIGITAL_KOMMUNIKASJON_ELLER_VARSLING
                                } else {
                                    KrrRegistrertStatus.IKKE_RESERVERT_MOT_DIGITAL_KOMMUNIKASJON_ELLER_VARSLING
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            statusEtterKallReservasjonsstatusBuilder
                .withRegistry(registry)
                .withTag("status", "failure")
            throw e
        } finally {
            sample.stop(responstidReservasjonsstatus)
        }
    }

    private fun JsonNode.getIfBoolean(fieldName: String): Boolean? = this[fieldName]?.takeIf(JsonNode::isBoolean)?.asBoolean()

    private fun JsonNode.getRequiredBoolean(
        fieldName: String,
        response: JsonNode,
    ): Boolean =
        getIfBoolean(fieldName)
            ?: responseError("Fant ikke boolean-verdi for feltet $fieldName", response.toString())

    private fun responseError(
        melding: String,
        response: String,
    ): Nothing {
        loggErrorWithNoThrowable(melding, "Full respons: $response")
        error(melding)
    }
}
