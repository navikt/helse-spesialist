package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode

class HentEnhetBehovLøser : AbstractBehovLøser("HentEnhet") {
    override fun løsning(behovJson: JsonNode) = "0301"
}
