package no.nav.helse.spesialist.client.speed

import io.micrometer.core.instrument.Metrics
import no.nav.helse.modell.vedtaksperiode.objectMapper
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.AlleIdenterHenter
import no.nav.helse.spesialist.application.Cache
import no.nav.helse.spesialist.application.hentGjennomCache
import no.nav.helse.spesialist.application.logg.loggDebug
import no.nav.helse.spesialist.application.logg.loggError
import no.nav.helse.spesialist.client.speed.dto.AlleIdenterRequest
import no.nav.helse.spesialist.client.speed.dto.AlleIdenterResponse
import no.nav.helse.spesialist.domain.Identitetsnummer
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.EntityUtils
import java.time.Duration
import java.util.UUID

class SpeedClientAlleIdenterHenter(
    private val configuration: ClientSpeedModule.Configuration,
    private val accessTokenGenerator: AccessTokenGenerator,
    private val cache: Cache,
) : AlleIdenterHenter {
    override fun hentAlleIdenter(identitetsnummer: Identitetsnummer): List<AlleIdenterHenter.Ident> =
        cache
            .hentGjennomCache(key = "speed-client:alle-identer:${identitetsnummer.value}", timeToLive = Duration.ofHours(1)) {
                timer.recordCallable { hentFraSpeed(identitetsnummer) }
            }?.identer
            .orEmpty()
            .map { it.tilDomene() }

    private fun hentFraSpeed(identitetsnummer: Identitetsnummer): AlleIdenterResponse? {
        val accessToken = accessTokenGenerator.hentAccessToken(configuration.scope)

        val uri = "${configuration.apiUrl}/api/alle_identer"
        loggDebug("Utfører HTTP POST $uri")

        val requestBody =
            objectMapper.writeValueAsString(AlleIdenterRequest(ident = identitetsnummer.value))

        return Request
            .post(uri)
            .setHeader("Authorization", "Bearer $accessToken")
            .setHeader("Accept", ContentType.APPLICATION_JSON.mimeType)
            .setHeader("callId", UUID.randomUUID().toString())
            .bodyString(requestBody, ContentType.APPLICATION_JSON)
            .execute()
            .handleResponse { response ->
                when (response.code) {
                    200 -> {
                        val responseBody = EntityUtils.toString(response.entity)
                        objectMapper.readValue(responseBody, AlleIdenterResponse::class.java)
                    }

                    404 -> null

                    in 500..599 -> {
                        val responseBody = EntityUtils.toString(response.entity).orEmpty()
                        error("Serverfeil fra Speed: ${response.code}, body=$responseBody")
                    }

                    else -> {
                        val responseBody = EntityUtils.toString(response.entity).orEmpty()
                        loggError("Feil ved henting av alle identer: status=${response.code}, body=$responseBody")
                        error("Feil fra Speed: ${response.code}")
                    }
                }
            }
    }

    private val timer =
        Metrics.timer(
            "spesialist.client.call.timer",
            "client",
            "speed",
            "operation",
            "hent-alle-identer",
        )
}

private fun AlleIdenterResponse.Ident.tilDomene(): AlleIdenterHenter.Ident =
    AlleIdenterHenter.Ident(
        ident = ident,
        type =
            when (type) {
                AlleIdenterResponse.IdentType.FOLKEREGISTERIDENT -> AlleIdenterHenter.IdentType.FOLKEREGISTERIDENT
                AlleIdenterResponse.IdentType.AKTORID -> AlleIdenterHenter.IdentType.AKTORID
                AlleIdenterResponse.IdentType.NPID -> AlleIdenterHenter.IdentType.NPID
            },
        gjeldende = gjeldende,
    )
