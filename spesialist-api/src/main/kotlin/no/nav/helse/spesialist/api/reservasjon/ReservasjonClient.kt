package no.nav.helse.spesialist.api.reservasjon

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.spesialist.api.client.AccessTokenClient
import no.nav.helse.spesialist.api.graphql.schema.Reservasjon
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

interface ReservasjonClient {
    suspend fun hentReservasjonsstatus(fnr: String): Reservasjon?
}

class KRRClient(
    private val httpClient: HttpClient,
    private val apiUrl: String,
    private val scope: String,
    private val accessTokenClient: AccessTokenClient,
) : ReservasjonClient {
    private val logg: Logger = LoggerFactory.getLogger(this.javaClass)
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    override suspend fun hentReservasjonsstatus(fnr: String): Reservasjon? {
        val sample = Timer.start(registry)
        return try {
            logg.debug("Henter accessToken")
            val accessToken = accessTokenClient.hentAccessToken(scope)
            val callId = UUID.randomUUID().toString()

            logg.debug("Henter reservasjon fra $apiUrl/rest/v1/person, callId=$callId")
            val reservasjon =
                httpClient
                    .get("$apiUrl/rest/v1/person") {
                        header("Authorization", "Bearer $accessToken")
                        header("Nav-Personident", fnr)
                        header("Nav-Call-Id", callId)
                        accept(ContentType.Application.Json)
                    }.body<Reservasjon>()

            statusEtterKallReservasjonsstatusBuilder
                .withRegistry(registry)
                .withTag("status", "success")
            reservasjon
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
}
