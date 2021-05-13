package no.nav.helse.risikovurdering

import com.fasterxml.jackson.databind.JsonNode

data class RisikovurderingApiDto(
    val funn: List<JsonNode>,
    val kontrollertOk: List<JsonNode>,
) {
    val arbeidsuførhetvurdering: List<String> = funn.map { it["beskrivelse"].asText() }
    val ufullstendig: Boolean = false
}
