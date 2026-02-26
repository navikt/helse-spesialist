package no.nav.helse.spesialist.client.spillkar

import no.nav.helse.modell.vedtaksperiode.objectMapper
import no.nav.helse.spesialist.application.AccessTokenGenerator
import no.nav.helse.spesialist.application.InngangsvilkårHenter
import no.nav.helse.spesialist.application.logg.loggDebug
import no.nav.helse.spesialist.application.logg.loggError
import no.nav.helse.spesialist.client.spillkar.generated.AutomatiskVurdertInngangsvilkårDetaljer
import no.nav.helse.spesialist.client.spillkar.generated.HentInngangsvilkår
import no.nav.helse.spesialist.client.spillkar.generated.ManueltVurdertInngangsvilkårDetaljer
import no.nav.helse.spesialist.client.spillkar.generated.SamlingAvVurderteInngangsvilkår
import no.nav.helse.spesialist.client.spillkar.generated.SamlingAvVurderteInngangsvilkårResponse
import no.nav.helse.spesialist.client.spillkar.generated.VurdertInngangsvilkårFelles
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.io.entity.EntityUtils
import java.time.LocalDate
import no.nav.helse.spesialist.application.spillkar.AutomatiskVurdering as DomeneAutomatiskVurdering
import no.nav.helse.spesialist.application.spillkar.SamlingAvVurderteInngangsvilkår as DomeneSamling
import no.nav.helse.spesialist.application.spillkar.VurdertInngangsvilkår as DomeneVurdertInngangsvilkår

class SpillkarClientInngangsvilkårHenter(
    private val configuration: ClientSpillkarModule.Configuration,
    private val accessTokenGenerator: AccessTokenGenerator,
) : InngangsvilkårHenter {
    override fun hentInngangsvilkår(
        personidentifikatorer: List<String>,
        skjæringstidspunkt: LocalDate,
    ): List<DomeneSamling> {
        val accessToken = accessTokenGenerator.hentAccessToken(configuration.scope)
        val uri = "${configuration.apiUrl}/vurderte-inngangsvilkar/alle"
        loggDebug("Utfører HTTP POST $uri")

        val requestBody =
            objectMapper.writeValueAsString(
                HentInngangsvilkår(
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

private fun SamlingAvVurderteInngangsvilkår.tilDomene() =
    DomeneSamling(
        samlingAvVurderteInngangsvilkårId = samlingAvVurderteInngangsvilkårId,
        versjon = versjon,
        skjæringstidspunkt = skjæringstidspunkt,
        vurderteInngangsvilkår = vurderteInngangsvilkår.map { it.tilDomene() },
    )

private fun VurdertInngangsvilkårFelles.tilDomene(): DomeneVurdertInngangsvilkår =
    when (this) {
        is AutomatiskVurdertInngangsvilkårDetaljer ->
            DomeneVurdertInngangsvilkår.AutomatiskVurdertInngangsvilkår(
                vilkårskode = vilkårskode,
                vurderingskode = vurderingskode,
                tidspunkt = tidspunkt.toInstant(),
                automatiskVurdering =
                    DomeneAutomatiskVurdering(
                        system = automatiskVurdering.system,
                        versjon = automatiskVurdering.versjon,
                        grunnlagsdata = automatiskVurdering.grunnlagsdata,
                    ),
            )

        is ManueltVurdertInngangsvilkårDetaljer ->
            DomeneVurdertInngangsvilkår.ManueltVurdertInngangsvilkår(
                vilkårskode = vilkårskode,
                vurderingskode = vurderingskode,
                tidspunkt = tidspunkt.toInstant(),
                navident = manuellVurdering.navident,
                begrunnelse = manuellVurdering.begrunnelse,
            )

        else ->
            throw IllegalArgumentException("Ukjent VurdertInngangsvilkårFelles-type: ${this::class.simpleName} for vilkårskode=$vilkårskode")
    }
