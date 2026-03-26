package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode

class VergemålBehovLøser : AbstractBehovLøser("Vergemål") {
    var vergemål: List<Map<String, Any>> = emptyList()
    var fremtidsfullmakter: List<Map<String, Any>> = emptyList()

    override fun løsning(behovJson: JsonNode) = mapOf(
        "vergemål" to vergemål,
        "fremtidsfullmakter" to fremtidsfullmakter,
        "fullmakter" to emptyList<Any>(),
    )
}
