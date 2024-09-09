package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

internal fun JsonNode.asUUID() = UUID.fromString(asText())

internal fun Map<String, String>.erPoisonPill(melding: JsonNode): Boolean {
    this.forEach { (key, value) ->
        val funnetVerdi = melding[key]?.asText() ?: return@forEach
        if (funnetVerdi == value) return true
    }
    return false
}
