package no.nav.helse.spesialist.client.spillkar

import no.nav.helse.modell.vedtaksperiode.objectMapper
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.InngangsvilkårInnsender
import no.nav.helse.spesialist.application.logg.loggDebug
import no.nav.helse.spesialist.application.logg.loggError
import no.nav.helse.spesialist.application.spillkar.`ManuelleInngangsvilkårVurderinger`
import no.nav.helse.spesialist.client.spillkar.dto.ManueltVurdertInngangsvilkårDto
import no.nav.helse.spesialist.client.spillkar.dto.ManueltVurderteInngangsvilkårRequest
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.EntityUtils

class SpillkarClientInngangsvilkårInnsender(
    private val configuration: ClientSpillkarModule.Configuration,
    private val accessTokenGenerator: AccessTokenGenerator,
) : InngangsvilkårInnsender {
    override fun sendManuelleVurderinger(vurderinger: ManuelleInngangsvilkårVurderinger) {
        val accessToken = accessTokenGenerator.hentAccessToken(configuration.scope)
        val uri = "${configuration.apiUrl}/vurderte-inngangsvilkar/manuelle-vurderinger"
        loggDebug("Utfører HTTP POST $uri")

        val requestBody =
            objectMapper.writeValueAsString(
                ManueltVurderteInngangsvilkårRequest(
                    personidentifikator = vurderinger.personidentifikator,
                    skjaeringstidspunkt = vurderinger.skjæringstidspunkt,
                    versjon = vurderinger.versjon,
                    vurderteInngangsvilkar =
                        vurderinger.vurderinger.map {
                            ManueltVurdertInngangsvilkårDto(
                                vilkårskode = it.vilkårskode,
                                vurderingskode = it.vurderingskode,
                                tidspunkt = it.tidspunkt,
                                begrunnelse = it.begrunnelse,
                            )
                        },
                ),
            )

        Request
            .post(uri)
            .setHeader("Authorization", "Bearer $accessToken")
            .setHeader("Accept", ContentType.APPLICATION_JSON.mimeType)
            .bodyString(requestBody, ContentType.APPLICATION_JSON)
            .execute()
            .handleResponse { response ->
                when (response.code) {
                    204 -> Unit

                    in 500..599 -> {
                        val responseBody = EntityUtils.toString(response.entity).orEmpty()
                        error("Serverfeil fra Spillkar: ${response.code}, body=$responseBody")
                    }

                    else -> {
                        val responseBody = EntityUtils.toString(response.entity).orEmpty()
                        loggError("Feil ved innsending av manuelle vurderinger: status=${response.code}, body=$responseBody")
                        error("Feil fra Spillkar: ${response.code}")
                    }
                }
            }
    }
}
