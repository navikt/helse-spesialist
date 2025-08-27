package no.nav.helse.spesialist.application

import no.nav.helse.FeatureToggles

class InMemoryFeatureToggles : FeatureToggles {
    var skalBehandleSelvstendig: Boolean = false

    override fun skalBehandleSelvstendig() = skalBehandleSelvstendig
}
