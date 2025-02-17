package no.nav.helse

interface FeatureToggles {
    fun skalAvbryteOppgavePåEtSenereTidspunkt(): Boolean = false

    fun skalBenytteNyTotrinnsvurderingsløsning(): Boolean = false

    fun skalBenytteNyAvviksvurderingløype(): Boolean = false
}
