package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode

class EgenAnsattBehovLøserStub : AbstractBehovLøserStub("EgenAnsatt") {
    override fun løsning(json: JsonNode): Map<String, Any?> = mapOf(
        "EgenAnsatt" to false
    )
}
