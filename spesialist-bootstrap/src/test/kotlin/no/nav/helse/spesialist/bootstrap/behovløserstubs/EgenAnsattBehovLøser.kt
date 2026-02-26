package no.nav.helse.spesialist.bootstrap.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode

class EgenAnsattBehovLøser : AbstractBehovLøser("EgenAnsatt") {
    override fun løsning(behovJson: JsonNode) = false
}
