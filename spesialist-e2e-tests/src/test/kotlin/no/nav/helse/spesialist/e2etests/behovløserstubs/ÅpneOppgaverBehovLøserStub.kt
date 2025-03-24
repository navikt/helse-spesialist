package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode

class ÅpneOppgaverBehovLøserStub : AbstractBehovLøserStub("ÅpneOppgaver") {
    override fun løsning(json: JsonNode): Map<String, Any?> = mapOf(
        "ÅpneOppgaver" to mapOf(
            "antall" to 0,
            "oppslagFeilet" to false
        )
    )
}
