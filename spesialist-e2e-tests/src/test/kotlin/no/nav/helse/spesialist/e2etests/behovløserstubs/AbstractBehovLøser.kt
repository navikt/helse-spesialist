package no.nav.helse.spesialist.e2etests.behovløserstubs

import tools.jackson.databind.JsonNode

abstract class AbstractBehovLøser(
    val behov: String,
) {
    abstract fun løsning(behovJson: JsonNode): Any?
}
