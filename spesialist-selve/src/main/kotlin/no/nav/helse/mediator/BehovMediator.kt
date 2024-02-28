package no.nav.helse.mediator

import java.util.UUID
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import org.slf4j.LoggerFactory

internal class BehovMediator {
    internal fun håndter(hendelse: Personmelding, context: CommandContext, contextId: UUID, messageContext: MessageContext) {
        publiserMeldinger(hendelse, context, messageContext)
        publiserBehov(hendelse, context, contextId, messageContext)
    }

    private fun publiserMeldinger(hendelse: Personmelding, context: CommandContext, messageContext: MessageContext) {
        context.meldinger().forEach { melding ->
            sikkerLogg.info("Sender melding i forbindelse med ${hendelse.javaClass.simpleName}\n{}", melding)
            messageContext.publish(hendelse.fødselsnummer(), melding)
        }
        context.nullstillMeldinger()
    }

    private fun publiserBehov(hendelse: Personmelding, context: CommandContext, contextId: UUID, messageContext: MessageContext) {
        if (!context.harBehov()) return
        val packet = behovPacket(hendelse, context, contextId)
        sikkerLogg.info("Sender behov for ${context.behov().keys}\n{}", packet)
        messageContext.publish(hendelse.fødselsnummer(), packet)
        context.nullstillBehov()
    }

    private fun behovPacket(hendelse: Personmelding, context: CommandContext, contextId: UUID) =
        JsonMessage.newNeed(context.behov().keys.toList(), mutableMapOf<String, Any>(
            "contextId" to contextId,
            "hendelseId" to hendelse.id,
            "fødselsnummer" to hendelse.fødselsnummer()
        ).apply {
            putAll(context.behov())
        }).toJson()

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }
}
