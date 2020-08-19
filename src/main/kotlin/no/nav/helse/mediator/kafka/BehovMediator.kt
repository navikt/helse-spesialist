package no.nav.helse.mediator.kafka

import no.nav.helse.mediator.kafka.meldinger.Hendelse
import no.nav.helse.modell.command.nyny.CommandContext
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.LocalDateTime
import java.util.*

internal class BehovMediator(
    private val rapidsConnection: RapidsConnection
) {
    internal fun håndter(hendelse: Hendelse, context: CommandContext) {
        if (!context.harBehov()) return
        rapidsConnection.publish(packet(hendelse, context))
    }

    private fun packet(hendelse: Hendelse, context: CommandContext) = standardfelter(hendelse).apply {
        this["@behov"] = context.behov().keys.toList()
        this["contextId"] = context.id
        this["hendelseId"] = hendelse.id
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
