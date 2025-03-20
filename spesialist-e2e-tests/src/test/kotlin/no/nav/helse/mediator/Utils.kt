package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

fun JsonNode.asUUID() = UUID.fromString(asText())
