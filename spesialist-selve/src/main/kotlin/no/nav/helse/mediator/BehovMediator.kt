package no.nav.helse.mediator

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.meldinger.Hendelse
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger

internal class BehovMediator(
    private val rapidsConnection: RapidsConnection,
    private val sikkerLogg: Logger
) {
    internal fun håndter(hendelse: Hendelse, context: CommandContext, contextId: UUID) {
        publiserMeldinger(hendelse, context)
        publiserBehov(hendelse, context, contextId)
    }

    private fun publiserMeldinger(hendelse: Hendelse, context: CommandContext) {
        context.meldinger().forEach { melding ->
            sikkerLogg.info("Sender melding i forbindelse med ${hendelse.javaClass.simpleName}\n{}", melding)
            val outgoing = hendelse.tracinginfo().takeUnless { it.isEmpty() }?.let { tracinginfo ->
                val node = objectMapper.readTree(melding) as ObjectNode
                node.replace("@forårsaket_av", objectMapper.valueToTree(tracinginfo))
                node.toString()
            } ?: melding
            rapidsConnection.publish(hendelse.fødselsnummer(), outgoing)
        }
    }

    private fun publiserBehov(hendelse: Hendelse, context: CommandContext, contextId: UUID) {
        if (!context.harBehov()) return
        val packet = behovPacket(hendelse, context, contextId)
        sikkerLogg.info("Sender behov for ${context.behov().keys}\n{}", packet)
        rapidsConnection.publish(hendelse.fødselsnummer(), packet)
    }

    private fun behovPacket(hendelse: Hendelse, context: CommandContext, contextId: UUID) =
        standardfelter(hendelse).apply {
            this["@behov"] = context.behov().keys.toList()
            this["contextId"] = contextId
            this["hendelseId"] = hendelse.id
            this["spleisBehovId"] =
                hendelse.id // only for BC because the need apps requires updating to use "hendelseId"
            putAll(context.behov())
        }.let { JsonMessage.newMessage(it).toJson() }

    private fun standardfelter(hendelse: Hendelse): MutableMap<String, Any> {
        val id = UUID.randomUUID()
        val fødselsnummer = hendelse.fødselsnummer()
        return mutableMapOf(
            "@event_name" to "behov",
            "@opprettet" to LocalDateTime.now(),
            "@id" to id,
            "fødselsnummer" to fødselsnummer
        )
    }
}
