package no.nav.helse.spesialist.api.risikovurdering

import tools.jackson.databind.JsonNode

data class RisikovurderingApiDto(
    val funn: List<JsonNode>,
    val kontrollertOk: List<JsonNode>,
)
