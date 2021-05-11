package no.nav.helse.modell.vedtaksperiode

import com.fasterxml.jackson.databind.JsonNode


data class RisikovurderingForSpeilDto(
    val funn: List<JsonNode>,
    val kontrollertOk: List<JsonNode>,
) {
    val arbeidsuførhetvurdering: List<String> = funn.map { it["beskrivelse"].asText() }
    val ufullstendig: Boolean = false
}

