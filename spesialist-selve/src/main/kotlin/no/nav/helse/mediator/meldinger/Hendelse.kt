package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import no.nav.helse.modell.kommando.Command
import no.nav.helse.objectMapper

internal interface Hendelse : Command {
    val id: UUID

    fun fødselsnummer(): String
    fun vedtaksperiodeId(): UUID? = null
    fun tracinginfo(): Map<String, Any> = objectMapper.readTree(toJson()).let { node ->
        mutableMapOf<String, Any>(
            "event_name" to node.path("@event_name").asText(),
            "id" to "$id",
            "opprettet" to node.path("@opprettet").asText()
        ).apply {
            compute("behov") { _, _ -> node.path("@behov").map(JsonNode::asText).takeUnless { it.isEmpty() } }
        }
    }
    fun toJson(): String
}

