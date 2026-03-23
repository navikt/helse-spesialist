package no.nav.helse.modell.vilkårsprøving

import java.time.LocalDateTime
import java.util.UUID

data class AvviksvurderingBehovLøsning(
    val avviksvurderingId: UUID,
    val maksimaltTillattAvvik: Double,
    val avviksprosent: Double,
    val harAkseptabeltAvvik: Boolean,
    val opprettet: LocalDateTime,
    val beregningsgrunnlag: Beregningsgrunnlag,
    val sammenligningsgrunnlag: Sammenligningsgrunnlag,
)
