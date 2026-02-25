package no.nav.helse.spesialist.client.spillkar.dto

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

internal data class HentInngangsvilkårRequest(
    val personidentifikatorer: List<String>,
    val skjæringstidspunkt: LocalDate,
)

internal data class SamlingAvVurderteInngangsvilkårResponse(
    val samlingAvVurderteInngangsvilkår: List<SamlingAvVurderteInngangsvilkårDto>,
)

internal data class SamlingAvVurderteInngangsvilkårDto(
    val samlingAvVurderteInngangsvilkårId: UUID,
    val versjon: Int,
    val skjæringstidspunkt: LocalDate,
    val vurderteInngangsvilkår: List<VurdertInngangsvilkårDto>,
)

internal data class VurdertInngangsvilkårDto(
    val vilkårskode: String,
    val vurderingskode: String?,
    val tidspunkt: Instant,
    val manuellVurdering: ManuellVurderingDto?,
    val automatiskVurdering: AutomatiskVurderingDto?,
)

internal data class ManuellVurderingDto(
    val navident: String,
    val begrunnelse: String,
)

internal data class AutomatiskVurderingDto(
    val system: String,
    val versjon: String,
    val grunnlagsdata: Map<String, String>,
)

internal data class ManueltVurderteInngangsvilkårRequest(
    val personidentifikator: String,
    val skjæringstidspunkt: LocalDate,
    val versjon: Int,
    val vurderteInngangsvilkår: List<ManueltVurdertInngangsvilkårDto>,
)

internal data class ManueltVurdertInngangsvilkårDto(
    val vilkårskode: String,
    val vurderingskode: String,
    val tidspunkt: Instant,
    val begrunnelse: String,
)
