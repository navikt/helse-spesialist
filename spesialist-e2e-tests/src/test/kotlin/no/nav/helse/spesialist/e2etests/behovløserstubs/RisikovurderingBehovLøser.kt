package no.nav.helse.spesialist.e2etests.behovløserstubs

import tools.jackson.databind.JsonNode

class RisikovurderingBehovLøser : AbstractBehovLøser("Risikovurdering") {
    var kanGodkjenneAutomatisk = true
    var funn = emptyList<Any>()
    var kontrollertOk = emptyList<Any>()
    var andreVedtaksperioder = emptyList<Map<String, Any>>()

    override fun løsning(behovJson: JsonNode) =
        buildMap {
            put("kanGodkjennesAutomatisk", kanGodkjenneAutomatisk)
            put("funn", funn)
            put("kontrollertOk", kontrollertOk)
            if (andreVedtaksperioder.isNotEmpty()) put("andreVedtaksperioder", andreVedtaksperioder)
        }
}
