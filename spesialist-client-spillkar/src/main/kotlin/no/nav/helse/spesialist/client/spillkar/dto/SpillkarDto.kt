package no.nav.helse.spesialist.client.spillkar.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal data class HentInngangsvilkårRequest(
    val personidentifikatorer: List<String>,
    @JsonProperty("skjæringstidspunkt")
    val skjaeringstidspunkt: LocalDate,
)

internal data class SamlingAvVurderteInngangsvilkårResponse(
    @JsonProperty("samlingAvVurderteInngangsvilkår")
    val samlingAvVurderteInngangsvilkar: List<SamlingAvVurderteInngangsvilkårDto>,
)

internal data class SamlingAvVurderteInngangsvilkårDto(
    val samlingAvVurderteInngangsvilkårId: UUID,
    val versjon: Int,
    @JsonProperty("skjæringstidspunkt")
    val skjaeringstidspunkt: LocalDate,
    @JsonProperty("vurderteInngangsvilkår")
    val vurderteInngangsvilkar: List<VurdertInngangsvilkårDto>,
)

internal data class VurdertInngangsvilkårDto(
    val vilkårskode: String,
    @JsonProperty("vurderingskode")
    val vurderingskode: String?,
    val tidspunkt: LocalDateTime,
    val manuellVurdering: ManuellVurderingDto?,
    val automatiskVurdering: AutomatiskVurderingDto?,
)

internal data class ManuellVurderingDto(
    val navident: String,
    val begrunnelse: String,
)

internal data class AutomatiskVurderingDto(
    val system: String?,
    val versjon: String?,
)

internal data class ManueltVurderteInngangsvilkårRequest(
    val personidentifikator: String,
    @JsonProperty("skjæringstidspunkt")
    val skjaeringstidspunkt: LocalDate,
    val versjon: Int,
    @JsonProperty("vurderteInngangsvilkår")
    val vurderteInngangsvilkar: List<ManueltVurdertInngangsvilkårDto>,
)

internal data class ManueltVurdertInngangsvilkårDto(
    val vilkårskode: String,
    val vurderingskode: String,
    val tidspunkt: LocalDateTime,
    val begrunnelse: String,
)
