package no.nav.helse.spesialist.e2etests.mockrivers

import com.fasterxml.jackson.databind.JsonNode

class EgenAnsattBehovMockRiver : AbstractBehovMockRiver("EgenAnsatt") {
    override fun løsning(json: JsonNode): Map<String, Any?> = mapOf(
        "EgenAnsatt" to false
    )
}
