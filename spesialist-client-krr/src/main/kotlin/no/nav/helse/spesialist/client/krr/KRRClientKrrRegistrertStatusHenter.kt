package no.nav.helse.spesialist.client.krr

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.KrrRegistrertStatusHenter
import no.nav.helse.spesialist.application.KrrRegistrertStatusHenter.KrrRegistrertStatus
import no.nav.helse.spesialist.application.logg.loggError
import no.nav.helse.spesialist.application.logg.loggInfo
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.util.Timeout
import java.time.Duration
import java.util.UUID

class KRRClientKrrRegistrertStatusHenter(
    private val configuration: ClientKrrModule.Configuration.Client,
    private val accessTokenGenerator: AccessTokenGenerator,
) : KrrRegistrertStatusHenter {
    private val objectMapper = jacksonObjectMapper()

    override fun hentForPerson(fødselsnummer: String): KrrRegistrertStatus =
        responstidHentReservasjonsstatusTimer.recordCallable {
            try {
                val accessToken = accessTokenGenerator.hentAccessToken(configuration.scope)
                val callId = UUID.randomUUID().toString()

                val uri = "${configuration.apiUrl}/rest/v1/personer"
                loggInfo("Utfører HTTP POST $uri med header Nav-Call-Id: $callId")
                Request
                    .post(uri)
                    .setHeader("Authorization", "Bearer $accessToken")
                    .setHeader("Nav-Call-Id", callId)
                    .setHeader("Accept", ContentType.APPLICATION_JSON.mimeType)
                    .connectTimeout(Timeout.of(Duration.ofSeconds(10)))
                    .responseTimeout(Timeout.of(Duration.ofSeconds(10)))
                    .bodyString(""" { "personidenter": [ "$fødselsnummer" ] } """, ContentType.APPLICATION_JSON)
                    .execute()
                    .handleResponse { response ->
                        val responseBody = EntityUtils.toString(response.entity)
                        if (response.code !in 200..299) {
                            responseError("Fikk HTTP ${response.code} tilbake fra KRR", responseBody)
                        }

                        val responseJson = objectMapper.readTree(responseBody)

                        if (responseJson["feil"]?.isEmpty == false) {
                            responseError("Fikk feil tilbake fra KRR", responseJson.toString())
                        } else {
                            responseJson["personer"][fødselsnummer].let {
                                if (it == null) {
                                    responseError(
                                        "Fant ikke igjen personen i responsen fra KRR",
                                        responseJson.toString(),
                                    )
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
                        }.also {
                            statusKallHentReservasjonsstatusCounter.withTag("status", "success").increment()
                        }
                    }
            } catch (e: Exception) {
                statusKallHentReservasjonsstatusCounter.withTag("status", "failure").increment()
                throw e
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
        loggError(melding, "response" to response)
        error(melding)
    }

    private val meterRegistry = Metrics.globalRegistry.add(PrometheusMeterRegistry(PrometheusConfig.DEFAULT))

    private val responstidHentReservasjonsstatusTimer =
        Timer
            .builder("responstid_hent_reservasjonsstatus")
            .description("Responstid for kall til digdir-krr-proxy")
            .register(meterRegistry)

    private val statusKallHentReservasjonsstatusCounter =
        Counter
            .builder("status_kall_hent_reservasjonsstatus")
            .description("Status på kall til digdir-krr-proxy, success eller failure")
            .withRegistry(meterRegistry)
}
