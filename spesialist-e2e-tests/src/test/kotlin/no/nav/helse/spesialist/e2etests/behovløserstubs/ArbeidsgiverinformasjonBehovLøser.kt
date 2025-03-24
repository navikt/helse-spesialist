package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode

class ArbeidsgiverinformasjonBehovLøser : AbstractBehovLøser("Arbeidsgiverinformasjon") {
    override fun løsning(behovJson: JsonNode) = behovJson["organisasjonsnummer"].map {
        mapOf(
            "orgnummer" to it.asText(),
            "navn" to "Navn for ${it.asText()}",
            "bransjer" to listOf("Bransje for ${it.asText()}")
        )
    }
}
