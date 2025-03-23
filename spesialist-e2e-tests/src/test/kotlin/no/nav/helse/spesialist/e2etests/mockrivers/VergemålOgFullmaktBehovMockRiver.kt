package no.nav.helse.spesialist.e2etests.mockrivers

import com.fasterxml.jackson.databind.JsonNode

class VergemålOgFullmaktBehovMockRiver : AbstractBehovMockRiver("Vergemål", "Fullmakt") {
    override fun løsning(json: JsonNode): Map<String, Any?> = mapOf(
        "Vergemål" to mapOf(
            "vergemål" to emptyList<Any>(),
            "fremtidsfullmakter" to emptyList(),
            "fullmakter" to emptyList(),
        ),
        "Fullmakt" to emptyList<Any>()
    )
}
