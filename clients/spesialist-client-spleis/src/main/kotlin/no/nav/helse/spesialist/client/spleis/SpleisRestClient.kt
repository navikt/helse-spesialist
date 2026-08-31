package no.nav.helse.spesialist.client.spleis

import com.github.navikt.tbd_libs.access_token.AccessTokenProvider
import io.micrometer.core.instrument.Metrics
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.application.logg.teamLogs
import no.nav.helse.spleis.rest.hentperson.Person
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpRequest
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.util.TimeValue
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.readValue
import java.net.URI
import java.util.UUID

/**
 * Klient for spleis sitt REST-endepunkt (`POST /api/person`) for å hente et snapshot av en person.
 */
internal class SpleisRestClient(
    private val accessTokenProvider: AccessTokenProvider,
    private val spleisUrl: URI,
    private val spleisClientId: String,
) {
    private val mapper: JsonMapper = jacksonMapperBuilder().build()

    class SpleisRestRetryStrategy : DefaultHttpRequestRetryStrategy(5, TimeValue.ofSeconds(1L)) {
        override fun handleAsIdempotent(request: HttpRequest) = true // Retry selv om det er POST
    }

    private val retryStrategy = SpleisRestRetryStrategy()

    fun hentPerson(fødselsnummer: String): Person? =
        HttpClientBuilder.create().setRetryStrategy(retryStrategy).build().use { client ->
            val callId = UUID.randomUUID().toString()
            val uri = spleisUrl.resolve("/api/person")
            val token = accessTokenProvider.machineToken(spleisClientId)
            val requestBody = """{"fødselsnummer": "$fødselsnummer"}"""
            loggInfo("Kaller HTTP POST $uri (REST) med callId $callId")
            timer.recordCallable {
                Request
                    .post(uri)
                    .setHeader("Authorization", "Bearer $token")
                    .setHeader("callId", callId)
                    .bodyString(requestBody, ContentType.APPLICATION_JSON)
                    .execute(client)
                    .handleResponse { response ->
                        when (response.code) {
                            404 -> null
                            in 200..299 -> {
                                val responseBody = EntityUtils.toString(response.entity)
                                mapper.readValue<Person>(responseBody)
                            }
                            else -> {
                                val responseBody = EntityUtils.toString(response.entity)
                                logg.error("Fikk HTTP ${response.code} i svar fra Spleis (REST). Se sikkerlogg for mer info.")
                                teamLogs.error("Fikk HTTP ${response.code}-svar fra Spleis (REST): $responseBody")
                                error("Uventet svar fra spleis sitt REST-endepunkt: ${response.code}")
                            }
                        }
                    }
            }
        }

    private val timer =
        Metrics.timer(
            "spesialist.client.call.timer",
            "client",
            "spleis",
            "operation",
            "hent-snapshot-rest",
        )
}
