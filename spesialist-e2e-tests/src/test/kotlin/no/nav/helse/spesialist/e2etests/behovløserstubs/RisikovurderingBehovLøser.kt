package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode

class RisikovurderingBehovLøser : AbstractBehovLøser("Risikovurdering") {
    override fun løsning(behovJson: JsonNode) = mapOf(
        "kanGodkjennesAutomatisk" to true,
        "funn" to emptyList<Any>(),
        "kontrollertOk" to emptyList<Any>(),
    )
}
