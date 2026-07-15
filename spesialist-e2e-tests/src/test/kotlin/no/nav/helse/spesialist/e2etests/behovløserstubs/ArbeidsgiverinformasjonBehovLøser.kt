package no.nav.helse.spesialist.e2etests.behovløserstubs

import tools.jackson.databind.JsonNode

class ArbeidsgiverinformasjonBehovLøser : AbstractBehovLøser("Arbeidsgiverinformasjon") {
    override fun løsning(behovJson: JsonNode) =
        behovJson["organisasjonsnummer"].toList().map {
            mapOf(
                "orgnummer" to it.asString(),
                "navn" to "Navn for ${it.asString()}",
            )
        }
}
