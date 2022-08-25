package no.nav.helse.modell.kommando

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.OverstyringMediator
import no.nav.helse.modell.overstyring.OverstyringDao
import no.nav.helse.objectMapper

internal class PubliserOverstyringCommand(
    private val eventName: String,
    private val hendelseId: UUID,
    private val json: String,
    private val overstyringMediator: OverstyringMediator,
    private val overstyringDao: OverstyringDao,
) : Command {
    override fun execute(context: CommandContext): Boolean {
        // Vi lagrer ned ekstern_hendelse_id før vi sender den ut. Derfor henter vi den ut eksplisitt her, slik at vi
        // kan garantere at vi bruker samme ID.
        val eksternHendelseId = overstyringDao.finnEksternHendelseIdFraHendelseId(hendelseId)

        val packet = objectMapper.readTree(json) as ObjectNode
        packet.put("@event_name", eventName)
        packet.put("@id", eksternHendelseId.toString())
        packet.replace("@opprettet", objectMapper.valueToTree(LocalDateTime.now()))
        overstyringMediator.sendOverstyring(packet)
        return true
    }
}
