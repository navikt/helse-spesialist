package no.nav.helse.modell.vilkårsprøving

import java.time.LocalDateTime
import java.util.UUID

sealed interface AvviksvurderingBehovLøsning {
    val avviksvurderingId: UUID

    data class TrengerIkkeNyVurdering(override val avviksvurderingId: UUID) : AvviksvurderingBehovLøsning

    data class NyVurderingForetatt(
        override val avviksvurderingId: UUID,
        val maksimaltTillattAvvik: Double,
        val avviksprosent: Double,
        val harAkseptabeltAvvik: Boolean,
        val opprettet: LocalDateTime,
        val beregningsgrunnlag: Beregningsgrunnlag,
        val sammenligningsgrunnlag: Sammenligningsgrunnlag,
    ) : AvviksvurderingBehovLøsning
}
