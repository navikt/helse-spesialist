package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode

class HentEnhetBehovLøserStub : AbstractBehovLøserStub("HentEnhet") {
    override fun løsning(json: JsonNode): Map<String, Any?> = mapOf(
        "HentEnhet" to "0301"
    )
}
