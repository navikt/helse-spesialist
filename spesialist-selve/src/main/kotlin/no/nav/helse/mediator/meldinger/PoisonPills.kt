package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode

class PoisonPills(private val posisonPills: Map<String, Set<String>>) {
    internal fun erPoisonPill(melding: JsonNode) =
        posisonPills
            .any { (key, poisonPillValues) ->
                melding[key]?.asText()?.let { verdiFraMelding ->
                    poisonPillValues.contains(verdiFraMelding)
                } ?: false
            }
}
