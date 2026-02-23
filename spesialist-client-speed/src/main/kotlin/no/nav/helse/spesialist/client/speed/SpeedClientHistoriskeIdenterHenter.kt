package no.nav.helse.spesialist.client.speed

import no.nav.helse.modell.vedtaksperiode.objectMapper
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.HistoriskeIdenterHenter
import no.nav.helse.spesialist.application.logg.loggDebug
import no.nav.helse.spesialist.application.logg.loggError
import no.nav.helse.spesialist.client.speed.dto.HistoriskeIdenterRequest
import no.nav.helse.spesialist.client.speed.dto.HistoriskeIdenterResponse
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.EntityUtils
import java.util.UUID

class SpeedClientHistoriskeIdenterHenter(
    private val configuration: ClientSpeedModule.Configuration,
    private val accessTokenGenerator: AccessTokenGenerator,
) : HistoriskeIdenterHenter {
    override fun hentHistoriskeIdenter(ident: String): List<String> {
        val accessToken = accessTokenGenerator.hentAccessToken(configuration.scope)
        val uri = "${configuration.apiUrl}/api/historiske_identer"
        loggDebug("Utfører HTTP POST $uri")

        val requestBody =
            objectMapper.writeValueAsString(HistoriskeIdenterRequest(ident = ident))

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
                        val dto = objectMapper.readValue(responseBody, HistoriskeIdenterResponse::class.java)
                        dto.fødselsnumre
                    }

                    404 -> emptyList()

                    in 500..599 -> {
                        val responseBody = EntityUtils.toString(response.entity).orEmpty()
                        error("Serverfeil fra Speed: ${response.code}, body=$responseBody")
                    }

                    else -> {
                        val responseBody = EntityUtils.toString(response.entity).orEmpty()
                        loggError("Feil ved henting av historiske identer: status=${response.code}, body=$responseBody")
                        error("Feil fra Speed: ${response.code}")
                    }
                }
            }
    }
}
