package no.nav.helse.spesialist.e2etests.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode

class ArbeidsgiverinformasjonBehovLøserStub : AbstractBehovLøserStub("Arbeidsgiverinformasjon") {
    override fun løsning(json: JsonNode): Map<String, Any?> = mapOf(
        "Arbeidsgiverinformasjon" to json["Arbeidsgiverinformasjon"]["organisasjonsnummer"].map {
            mapOf(
                "orgnummer" to it.asText(),
                "navn" to "Navn for ${it.asText()}",
                "bransjer" to listOf("Bransje for ${it.asText()}")
            )
        }
    )
}
