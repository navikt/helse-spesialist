package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode

class EgenAnsattBehovLøser : AbstractBehovLøser("EgenAnsatt") {
    override fun løsning(behovJson: JsonNode) = false
}
