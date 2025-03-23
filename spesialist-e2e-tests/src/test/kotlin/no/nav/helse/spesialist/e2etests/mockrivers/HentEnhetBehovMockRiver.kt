package no.nav.helse.spesialist.e2etests.mockrivers

import com.fasterxml.jackson.databind.JsonNode

class HentEnhetBehovMockRiver : AbstractBehovMockRiver("HentEnhet") {
    override fun løsning(json: JsonNode): Map<String, Any?> = mapOf(
        "HentEnhet" to "0301"
    )
}
