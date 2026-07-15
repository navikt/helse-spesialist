package no.nav.helse.spesialist.e2etests.behovløserstubs

import tools.jackson.databind.JsonNode

class HentEnhetBehovLøser : AbstractBehovLøser("HentEnhet") {
    var enhet = "0301"

    override fun løsning(behovJson: JsonNode) = enhet
}
