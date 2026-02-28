package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode

class ÅpneOppgaverBehovLøser : AbstractBehovLøser("ÅpneOppgaver") {
    var antall = 0
    var oppslagFeilet = false

    override fun løsning(behovJson: JsonNode) = mapOf(
        "antall" to antall,
        "oppslagFeilet" to oppslagFeilet
    )
}
