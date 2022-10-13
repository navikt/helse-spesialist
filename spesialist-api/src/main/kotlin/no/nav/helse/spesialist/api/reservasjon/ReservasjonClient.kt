package no.nav.helse.spesialist.api.reservasjon

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.prometheus.client.Histogram
import java.util.UUID
import no.nav.helse.spesialist.api.client.AccessTokenClient
import no.nav.helse.spesialist.api.graphql.schema.Reservasjon
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ReservasjonClient(
    private val httpClient: HttpClient,
    private val apiUrl: String,
    private val scope: String,
    private val accessTokenClient: AccessTokenClient,
) {
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    companion object {
        private val responstidReservasjonsstatus: Histogram = Histogram.build()
            .name("responstid_hent_reservasjonsstatus")
            .help("Responstid for kall til digdir-krr-proxy")
            .register()
    }

    internal suspend fun hentReservasjonsstatus(fnr: String): Reservasjon? {
        val timer = responstidReservasjonsstatus.startTimer()
        try {
            val accessToken = accessTokenClient.hentAccessToken(scope)
            val callId = UUID.randomUUID().toString()

            return httpClient.get("$apiUrl/rest/v1/person") {
                header("Authorization", "Bearer $accessToken")
                header("Nav-Personident", fnr)
                header("Nav-Call-Id", callId)
                accept(ContentType.Application.Json)
            }.body()
        } catch (e: Exception) {
            sikkerLogg.error("Feil under kall til Kontakt- og reservasjonsregisteret:", e)
        } finally {
            timer.observeDuration()
        }

        return null
    }

}