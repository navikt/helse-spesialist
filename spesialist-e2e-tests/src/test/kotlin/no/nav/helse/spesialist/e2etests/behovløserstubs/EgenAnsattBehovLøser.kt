package no.nav.helse.spesialist.e2etests.behovløserstubs

import tools.jackson.databind.JsonNode

class EgenAnsattBehovLøser : AbstractBehovLøser("EgenAnsatt") {
    var erEgenAnsatt = false

    override fun løsning(behovJson: JsonNode) = erEgenAnsatt
}
