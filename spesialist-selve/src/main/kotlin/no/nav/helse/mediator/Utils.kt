package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

internal fun JsonNode.asUUID() = UUID.fromString(asText())

internal fun Map<String, Set<String>>.erPoisonPill(melding: JsonNode): Boolean {
    this.forEach { (key, values) ->
        val funnetVerdi = melding[key]?.asText() ?: return@forEach
        if (values.contains(funnetVerdi)) return true
    }
    return false
}
