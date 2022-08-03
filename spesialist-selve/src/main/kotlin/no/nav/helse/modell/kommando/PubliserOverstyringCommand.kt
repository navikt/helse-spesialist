package no.nav.helse.modell.kommando

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.OverstyringMediator
import no.nav.helse.objectMapper

internal class PubliserOverstyringCommand(
    private val eventName: String,
    private val json: String,
    private val overstyringMediator: OverstyringMediator,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        val packet = objectMapper.readTree(json) as ObjectNode
        packet.put("@event_name", eventName)
        packet.put("@id", UUID.randomUUID().toString())
        packet.replace("@opprettet", objectMapper.valueToTree(LocalDateTime.now()))
        overstyringMediator.sendOverstyring(packet)
        return true
    }
}
