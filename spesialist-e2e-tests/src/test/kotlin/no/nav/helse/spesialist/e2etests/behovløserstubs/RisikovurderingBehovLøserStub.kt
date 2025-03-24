package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode

class RisikovurderingBehovLøserStub : AbstractBehovLøserStub("Risikovurdering") {
    override fun løsning(json: JsonNode): Map<String, Any?> = mapOf(
        "Risikovurdering" to mapOf(
            "kanGodkjennesAutomatisk" to false,
            "funn" to emptyList<Any>(),
            "kontrollertOk" to emptyList<Any>(),
        )
    )
}
