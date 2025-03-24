package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode

class ÅpneOppgaverBehovLøser : AbstractBehovLøser("ÅpneOppgaver") {
    override fun løsning(behovJson: JsonNode) = mapOf(
        "antall" to 0,
        "oppslagFeilet" to false
    )
}
