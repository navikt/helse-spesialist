package no.nav.helse.spesialist.client.spillkar

import no.nav.helse.modell.vedtaksperiode.objectMapper
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.InngangsvilkårHenter
import no.nav.helse.spesialist.application.logg.loggDebug
import no.nav.helse.spesialist.application.logg.loggError
import no.nav.helse.spesialist.application.spillkar.AutomatiskVurdering
import no.nav.helse.spesialist.application.spillkar.`SamlingAvVurderteInngangsvilkår`
import no.nav.helse.spesialist.application.spillkar.`VurdertInngangsvilkår`
import no.nav.helse.spesialist.client.spillkar.dto.HentInngangsvilkårRequest
import no.nav.helse.spesialist.client.spillkar.dto.SamlingAvVurderteInngangsvilkårDto
import no.nav.helse.spesialist.client.spillkar.dto.SamlingAvVurderteInngangsvilkårResponse
import no.nav.helse.spesialist.client.spillkar.dto.VurdertInngangsvilkårDto
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.EntityUtils
import java.time.LocalDate

class SpillkarClientInngangsvilkårHenter(
    private val configuration: ClientSpillkarModule.Configuration,
    private val accessTokenGenerator: AccessTokenGenerator,
) : InngangsvilkårHenter {
    override fun hentInngangsvilkår(
        personidentifikatorer: List<String>,
        skjæringstidspunkt: LocalDate,
    ): List<SamlingAvVurderteInngangsvilkår> {
        val accessToken = accessTokenGenerator.hentAccessToken(configuration.scope)
        val uri = "${configuration.apiUrl}/vurderte-inngangsvilkar/alle"
        loggDebug("Utfører HTTP POST $uri")

        val requestBody =
            objectMapper.writeValueAsString(
                HentInngangsvilkårRequest(
                    personidentifikatorer = personidentifikatorer,
                    skjæringstidspunkt = skjæringstidspunkt,
                ),
            )

        return Request
            .post(uri)
            .setHeader("Authorization", "Bearer $accessToken")
            .setHeader("Accept", ContentType.APPLICATION_JSON.mimeType)
            .bodyString(requestBody, ContentType.APPLICATION_JSON)
            .execute()
            .handleResponse { response ->
                when (response.code) {
                    200 -> {
                        val responseBody = EntityUtils.toString(response.entity)
                        val dto =
                            objectMapper.readValue(responseBody, SamlingAvVurderteInngangsvilkårResponse::class.java)
                        dto.samlingAvVurderteInngangsvilkår.map { it.tilDomene() }
                    }

                    in 500..599 -> {
                        val responseBody = EntityUtils.toString(response.entity).orEmpty()
                        error("Serverfeil fra Spillkar: ${response.code}, body=$responseBody")
                    }

                    else -> {
                        val responseBody = EntityUtils.toString(response.entity).orEmpty()
                        loggError("Feil ved henting av inngangsvilkår: status=${response.code}, body=$responseBody")
                        error("Feil fra Spillkar: ${response.code}")
                    }
                }
            }
    }
}

private fun SamlingAvVurderteInngangsvilkårDto.tilDomene() =
    SamlingAvVurderteInngangsvilkår(
        samlingAvVurderteInngangsvilkårId = samlingAvVurderteInngangsvilkårId,
        versjon = versjon,
        skjæringstidspunkt = skjæringstidspunkt,
        vurderteInngangsvilkår = vurderteInngangsvilkår.map { it.tilDomene() },
    )

private fun VurdertInngangsvilkårDto.tilDomene(): VurdertInngangsvilkår =
    when {
        manuellVurdering != null && automatiskVurdering != null ->
            throw IllegalArgumentException("VurdertInngangsvilkår har både manuellVurdering og automatiskVurdering for vilkårskode=$vilkårskode, det skal ikke skje")

        manuellVurdering != null ->
            VurdertInngangsvilkår.ManueltVurdertInngangsvilkår(
                vilkårskode = vilkårskode,
                vurderingskode = vurderingskode,
                tidspunkt = tidspunkt,
                navident = manuellVurdering.navident,
                begrunnelse = manuellVurdering.begrunnelse,
            )

        automatiskVurdering != null ->
            VurdertInngangsvilkår.AutomatiskVurdertInngangsvilkår(
                vilkårskode = vilkårskode,
                vurderingskode = vurderingskode,
                tidspunkt = tidspunkt,
                automatiskVurdering =
                    AutomatiskVurdering(
                        system = automatiskVurdering.system,
                        versjon = automatiskVurdering.versjon,
                        grunnlagsdata = automatiskVurdering.grunnlagsdata,
                    ),
            )

        else ->
            throw IllegalArgumentException("VurdertInngangsvilkår mangler både manuellVurdering og automatiskVurdering for vilkårskode=$vilkårskode")
    }
