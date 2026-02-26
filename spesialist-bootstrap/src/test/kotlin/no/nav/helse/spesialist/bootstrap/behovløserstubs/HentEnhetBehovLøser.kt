package no.nav.helse.spesialist.bootstrap.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode

class HentEnhetBehovLøser : AbstractBehovLøser("HentEnhet") {
    override fun løsning(behovJson: JsonNode) = "0301"
}
