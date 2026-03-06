package no.nav.helse.spesialist.client.sparkel.norg

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.micrometer.core.instrument.Metrics.globalRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusConfig.DEFAULT
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.BehandlendeEnhetHenter
import no.nav.helse.spesialist.application.Cache
import no.nav.helse.spesialist.application.hentGjennomCache
import no.nav.helse.spesialist.application.logg.loggError
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Enhet
import no.nav.helse.spesialist.domain.Identitetsnummer
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.EntityUtils
import java.time.Duration
import java.util.UUID

class SparkelNorgClientBehandlendeEnhetHenter(
    private val configuration: ClientSparkelNorgModule.Configuration,
    private val accessTokenGenerator: AccessTokenGenerator,
    private val cache: Cache,
) : BehandlendeEnhetHenter {
    private val objectMapper = jacksonObjectMapper()

    override fun hentFor(identitetsnummer: Identitetsnummer): Enhet? =
        cache.hentGjennomCache(
            key = "sparkel-norg-client:behandlende-enhet:${identitetsnummer.value}",
            timeToLive = Duration.ofHours(24),
        ) {
            timer.recordCallable {
                hentFraSparkelNorg(identitetsnummer.value)
            }
        }

    private fun hentFraSparkelNorg(fødselsnummer: String): Enhet? {
        val accessToken = accessTokenGenerator.hentAccessToken(configuration.scope)
        val callId = UUID.randomUUID().toString()

        val uri = "${configuration.apiUrl}/api/behandlende-enhet"
        loggInfo("Utfører HTTP POST $uri med header Nav-Call-Id: $callId")
        return Request
            .post(uri)
            .setHeader("Authorization", "Bearer $accessToken")
            .setHeader("X-Request-ID", callId)
            .setHeader("Accept", ContentType.APPLICATION_JSON.mimeType)
            .bodyString("""{ "fødselsnummer": "$fødselsnummer" }""", ContentType.APPLICATION_JSON)
            .execute()
            .handleResponse { response ->
                if (response.code == 404) return@handleResponse null

                val responseBody = EntityUtils.toString(response.entity)
                if (response.code !in 200..299) {
                    responseError("Fikk HTTP ${response.code} tilbake fra sparkel-norg", responseBody)
                }

                val responseJson = objectMapper.readTree(responseBody)
                Enhet(
                    enhetNr =
                        responseJson["enhetNr"]?.asText()
                            ?: responseError("Fant ikke feltet enhetNr i responsen fra sparkel-norg", responseBody),
                    navn =
                        responseJson["navn"]?.asText()
                            ?: responseError("Fant ikke feltet navn i responsen fra sparkel-norg", responseBody),
                    type =
                        responseJson["type"]?.asText()
                            ?: responseError("Fant ikke feltet type i responsen fra sparkel-norg", responseBody),
                )
            }
    }

    private fun responseError(
        melding: String,
        response: String,
    ): Nothing {
        loggError(melding, "response" to response)
        error(melding)
    }

    private val timer =
        Timer
            .builder("client.sparkel.norg.kall")
            .description("Tidsbruk på kall til sparkel-norg")
            .register(globalRegistry.add(PrometheusMeterRegistry(DEFAULT)))
}
