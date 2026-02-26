package no.nav.helse.spesialist.bootstrap.behovløserstubs

import com.fasterxml.jackson.databind.JsonNode

abstract class AbstractBehovLøser(
    val behov: String,
) {
    abstract fun løsning(behovJson: JsonNode): Any?
}
