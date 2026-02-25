package no.nav.helse.spesialist.application.spillkar

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class SamlingAvVurderteInngangsvilkår(
    val samlingAvVurderteInngangsvilkårId: UUID,
    val versjon: Int,
    val skjæringstidspunkt: LocalDate,
    val vurderteInngangsvilkår: List<VurdertInngangsvilkår>,
)

sealed class VurdertInngangsvilkår {
    abstract val vilkårskode: String
    abstract val vurderingskode: String?
    abstract val tidspunkt: Instant

    data class ManueltVurdertInngangsvilkår(
        override val vilkårskode: String,
        override val vurderingskode: String?,
        override val tidspunkt: Instant,
        val navident: String,
        val begrunnelse: String,
    ) : VurdertInngangsvilkår()

    data class AutomatiskVurdertInngangsvilkår(
        override val vilkårskode: String,
        override val vurderingskode: String?,
        override val tidspunkt: Instant,
        val automatiskVurdering: AutomatiskVurdering,
    ) : VurdertInngangsvilkår()
}

data class AutomatiskVurdering(
    val system: String,
    val versjon: String,
    val grunnlagsdata: Map<String, String>,
)
