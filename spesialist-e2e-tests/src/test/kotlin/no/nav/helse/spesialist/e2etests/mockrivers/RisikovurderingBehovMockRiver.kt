package no.nav.helse.spesialist.e2etests.mockrivers

import com.fasterxml.jackson.databind.JsonNode

class RisikovurderingBehovMockRiver : AbstractBehovMockRiver("Risikovurdering") {
    override fun løsning(json: JsonNode): Map<String, Any?> = mapOf(
        "Risikovurdering" to mapOf(
            "kanGodkjennesAutomatisk" to false,
            "funn" to emptyList<Any>(),
            "kontrollertOk" to emptyList<Any>(),
        )
    )
}
