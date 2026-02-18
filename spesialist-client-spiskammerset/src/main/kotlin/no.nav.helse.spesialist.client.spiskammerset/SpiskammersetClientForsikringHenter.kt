package no.nav.helse.spesialist.client.spiskammerset

import no.nav.helse.modell.vedtaksperiode.objectMapper
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.ForsikringHenter
import no.nav.helse.spesialist.application.logg.loggError
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Forsikring
import no.nav.helse.spesialist.domain.ResultatAvForsikring
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.EntityUtils
import java.util.UUID

class SpiskammersetClientForsikringHenter(
    private val configuration: ClientSpiskammersetModule.Configuration,
    private val accessTokenGenerator: AccessTokenGenerator,
) : ForsikringHenter {
    override fun hentForsikringsinformasjon(spleisBehandlingId: SpleisBehandlingId): ResultatAvForsikring {
        val accessToken = accessTokenGenerator.hentAccessToken(configuration.scope)
        val callId = UUID.randomUUID().toString()
        val uri = "${configuration.apiUrl}/behandling/${spleisBehandlingId.value}/forsikring"
        loggInfo("Utfører HTTP GET $uri med header Call-Id: $callId")

        return Request
            .get(uri)
            .setHeader("Authorization", "Bearer $accessToken")
            .setHeader("callId", callId)
            .setHeader("Accept", ContentType.APPLICATION_JSON.mimeType)
            .execute()
            .handleResponse { response ->
                when (response.code) {
                    204 -> ResultatAvForsikring.IngenForsikring
                    200 -> {
                        val responseBody = EntityUtils.toString(response.entity)
                        val responseJson = objectMapper.readTree(responseBody)
                        ResultatAvForsikring.MottattForsikring(
                            forsikring =
                                Forsikring.Factory.ny(
                                    gjelderFraDag = responseJson["dag1Eller17"].asInt(),
                                    dekningsgrad = responseJson["dekningsgrad"].asInt(),
                                ),
                        )
                    }
                    else -> {
                        val responseBody = EntityUtils.toString(response.entity).orEmpty()
                        loggError("Feil ved henting av forsikring: status=${response.code}, body=$responseBody")
                        throw RuntimeException("Feil fra forsikringstjeneste: ${response.code}")
                    }
                }
            }
    }
}
