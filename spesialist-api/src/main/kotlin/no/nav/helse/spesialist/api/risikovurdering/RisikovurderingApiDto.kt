package no.nav.helse.spesialist.api.risikovurdering

import com.fasterxml.jackson.databind.JsonNode

data class RisikovurderingApiDto(
    val funn: List<JsonNode>,
    val kontrollertOk: List<JsonNode>,
) {
    val arbeidsuførhetvurdering: List<String> = funn.map { it["beskrivelse"].asText() }
}
