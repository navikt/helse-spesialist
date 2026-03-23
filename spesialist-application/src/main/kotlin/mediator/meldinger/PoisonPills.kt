package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode

class PoisonPills(
    private val poisonPills: Map<String, Set<String>>,
) {
    fun erPoisonPill(melding: JsonNode) =
        poisonPills
            .any { (key, poisonPillValues) ->
                melding[key]?.asText()?.let { verdiFraMelding ->
                    poisonPillValues.contains(verdiFraMelding)
                } ?: false
            }
}
