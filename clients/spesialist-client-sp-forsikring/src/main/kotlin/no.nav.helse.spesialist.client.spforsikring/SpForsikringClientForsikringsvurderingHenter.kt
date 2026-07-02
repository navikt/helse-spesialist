package no.nav.helse.spesialist.client.spforsikring

import com.github.navikt.tbd_libs.access_token.AccessTokenProvider
import io.micrometer.core.instrument.Metrics
import no.nav.helse.modell.objectMapper
import no.nav.helse.spesialist.application.Forsikringsvurdering
import no.nav.helse.spesialist.application.ForsikringsvurderingHenter
import no.nav.helse.spesialist.application.logg.loggError
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.client.spforsikring.ClientUtils.Companion.retryMedBackoff
import no.nav.helse.spesialist.domain.ForsikringsvurderingId
import no.nav.helse.spesialist.domain.Identitetsnummer
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.EntityUtils
import java.util.UUID

class SpForsikringClientForsikringsvurderingHenter(
    private val configuration: ClientSpForsikringModule.Configuration,
    private val accessTokenProvider: AccessTokenProvider,
) : ForsikringsvurderingHenter {
    override fun hent(forsikringsvurderingId: ForsikringsvurderingId): Forsikringsvurdering? {
        val accessToken = accessTokenProvider.machineToken(configuration.scope)
        val callId = UUID.randomUUID().toString()
        val uri = "${configuration.apiUrl}/forsikringsvurderinger/${forsikringsvurderingId.value}"
        loggInfo("Utfører HTTP GET $uri med header Call-Id: $callId")

        return timer.recordCallable {
            retryMedBackoff {
                Request
                    .get(uri)
                    .setHeader("Authorization", "Bearer $accessToken")
                    .setHeader("callId", callId)
                    .setHeader("Accept", ContentType.APPLICATION_JSON.mimeType)
                    .execute()
                    .handleResponse { response ->
                        when (response.code) {
                            200 -> {
                                val responseBody = EntityUtils.toString(response.entity)
                                val responseJson = objectMapper.readTree(responseBody)
                                Forsikringsvurdering(
                                    identitetsnummer = Identitetsnummer.fraString(responseJson["identitetsnummer"].asText()),
                                    harForsikring = responseJson["harForsikring"].asBoolean(),
                                    dekning =
                                        responseJson["dekning"]?.takeUnless { it.isNull }?.let { dekning ->
                                            Forsikringsvurdering.Dekning(
                                                grad = dekning["grad"].asInt(),
                                                fraDag = dekning["fraDag"].asInt(),
                                            )
                                        },
                                )
                            }

                            404 -> {
                                null
                            }

                            in 500..599 -> {
                                val responseBody = EntityUtils.toString(response.entity).orEmpty()
                                throw RetryableException("Serverfeil fra forsikringstjeneste: ${response.code}, body=$responseBody")
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
    }

    private val timer =
        Metrics.timer(
            "spesialist.client.call.timer",
            "client",
            "sp-forsikring",
            "operation",
            "hent-forsikring",
        )
}
