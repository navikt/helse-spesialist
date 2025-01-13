package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.MeldingPubliserer
import no.nav.helse.kafka.message_builders.somJsonMessage
import no.nav.helse.mediator.KommandokjedeEndretEvent
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.UtgåendeHendelse
import org.slf4j.LoggerFactory
import java.util.UUID

class MessageContextMeldingPubliserer(private val context: MessageContext) : MeldingPubliserer {
    override fun publiser(
        fødselsnummer: String,
        hendelse: UtgåendeHendelse,
        hendelseNavn: String,
    ) {
        val packet = hendelse.somJsonMessage(fødselsnummer).toJson()
        logg.info("Publiserer hendelse i forbindelse med $hendelseNavn")
        sikkerlogg.info("Publiserer hendelse i forbindelse med $hendelseNavn\n{}", packet)
        context.publish(packet)
    }

    override fun publiser(
        hendelseId: UUID,
        commandContextId: UUID,
        fødselsnummer: String,
        behov: Map<String, Behov>,
    ) {
        val packet = behov.values.somJsonMessage(commandContextId, fødselsnummer, hendelseId).toJson()
        logg.info("Publiserer behov for ${behov.keys}")
        sikkerlogg.info("Publiserer behov for ${behov.keys}\n{}", packet)
        context.publish(packet)
    }

    override fun publiser(
        event: KommandokjedeEndretEvent,
        hendelseNavn: String,
    ) {
        val message = JsonMessage.newMessage(event.eventName, event.detaljer()).toJson()
        logg.info(
            "Publiserer CommandContext tilstandendring i forbindelse med $hendelseNavn, ny tilstand: ${event::class.simpleName}",
        )
        sikkerlogg.info(
            "Publiserer CommandContext tilstandendring i forbindelse med $hendelseNavn, ny tilstand: $${event::class.simpleName}\n{}",
            message,
        )
        context.publish(message)
    }

    private companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
