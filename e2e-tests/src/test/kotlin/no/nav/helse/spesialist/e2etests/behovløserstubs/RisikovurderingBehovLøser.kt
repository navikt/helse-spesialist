package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode

class RisikovurderingBehovLøser : AbstractBehovLøser("Risikovurdering") {
    var kanGodkjenneAutomatisk = true
    var funn = emptyList<Any>()
    var kontrollertOk = emptyList<Any>()

    override fun løsning(behovJson: JsonNode) = mapOf(
        "kanGodkjennesAutomatisk" to kanGodkjenneAutomatisk,
        "funn" to funn,
        "kontrollertOk" to kontrollertOk,
    )
}
