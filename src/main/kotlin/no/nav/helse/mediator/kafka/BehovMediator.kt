package no.nav.helse.mediator.kafka

import no.nav.helse.mediator.kafka.meldinger.Hendelse
import no.nav.helse.modell.command.nyny.CommandContext
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import java.time.LocalDateTime
import java.util.*

internal class BehovMediator(
    private val rapidsConnection: RapidsConnection,
    private val sikkerLogg: Logger
) {
    internal fun håndter(hendelse: Hendelse, context: CommandContext, contextId: UUID) {
        publiserMeldinger(hendelse, context)
        publiserBehov(hendelse, context, contextId)
    }

    private fun publiser(hendelse: Hendelse, melding: String) {
        rapidsConnection.publish(hendelse.fødselsnummer(), melding.also { sikkerLogg.info("sender {}", it) })
    }

    private fun publiserMeldinger(hendelse: Hendelse, context: CommandContext) {
        context.meldinger().forEach { melding -> publiser(hendelse, melding) }
    }

    private fun publiserBehov(hendelse: Hendelse, context: CommandContext, contextId: UUID) {
        if (!context.harBehov()) return
        publiser(hendelse, packet(hendelse, context, contextId))
    }

    private fun packet(hendelse: Hendelse, context: CommandContext, contextId: UUID) = standardfelter(hendelse).apply {
        this["@behov"] = context.behov().keys.toList()
        this["contextId"] = contextId
        this["vedtaksperiodeId"] = UUID.randomUUID() // TODO: Behovappene bruker bare "vedtaksperiodeId" til logging
        this["hendelseId"] = hendelse.id
        this["spleisBehovId"] = hendelse.id // only for BC because the need apps requires updating to use "hendelseId"
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
