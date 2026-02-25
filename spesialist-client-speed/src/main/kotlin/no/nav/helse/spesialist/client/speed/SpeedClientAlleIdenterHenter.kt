package no.nav.helse.spesialist.client.speed

import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.modell.vedtaksperiode.objectMapper
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.AlleIdenterHenter
import no.nav.helse.spesialist.application.logg.loggDebug
import no.nav.helse.spesialist.application.logg.loggError
import no.nav.helse.spesialist.client.speed.dto.AlleIdenterRequest
import no.nav.helse.spesialist.client.speed.dto.AlleIdenterResponse
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.EntityUtils
import java.util.UUID

class SpeedClientAlleIdenterHenter(
    private val configuration: ClientSpeedModule.Configuration,
    private val accessTokenGenerator: AccessTokenGenerator,
    private val environmentToggles: EnvironmentToggles,
) : AlleIdenterHenter {
    override fun hentAlleIdenter(ident: String): List<AlleIdenterHenter.Ident> {
        val accessToken =
            if (environmentToggles.devGcp) {
                "whatever-token"
            } else {
                accessTokenGenerator.hentAccessToken(configuration.scope)
            }
        val uri = "${configuration.apiUrl}/api/alle_identer"
        loggDebug("Utfører HTTP POST $uri")

        val requestBody =
            objectMapper.writeValueAsString(AlleIdenterRequest(ident = ident))

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
                        loggDebug("Fikk 200 OK fra Speed ved henting av alle identer, body=$responseBody")
                        val dto = objectMapper.readValue(responseBody, AlleIdenterResponse::class.java)
                        dto.identer.map { it.tilDomene() }
                    }

                    404 -> emptyList()

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
    )
