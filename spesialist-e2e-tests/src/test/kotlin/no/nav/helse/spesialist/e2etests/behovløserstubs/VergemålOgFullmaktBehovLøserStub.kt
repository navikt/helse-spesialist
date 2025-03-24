package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode

class VergemålOgFullmaktBehovLøserStub : AbstractBehovLøserStub("Vergemål", "Fullmakt") {
    override fun løsning(json: JsonNode): Map<String, Any?> = mapOf(
        "Vergemål" to mapOf(
            "vergemål" to emptyList<Any>(),
            "fremtidsfullmakter" to emptyList(),
            "fullmakter" to emptyList(),
        ),
        "Fullmakt" to emptyList<Any>()
    )
}
