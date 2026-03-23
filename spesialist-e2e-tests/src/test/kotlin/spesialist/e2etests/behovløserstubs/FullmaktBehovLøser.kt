package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode

class FullmaktBehovLøser : AbstractBehovLøser("Fullmakt") {
    override fun løsning(behovJson: JsonNode) = emptyList<Any>()
}
