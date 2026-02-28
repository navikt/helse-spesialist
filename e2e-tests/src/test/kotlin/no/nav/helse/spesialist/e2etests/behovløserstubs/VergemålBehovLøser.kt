package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode

class VergemålBehovLøser : AbstractBehovLøser("Vergemål") {
    override fun løsning(behovJson: JsonNode) = mapOf(
        "vergemål" to emptyList<Any>(),
        "fremtidsfullmakter" to emptyList(),
        "fullmakter" to emptyList(),
    )
}
