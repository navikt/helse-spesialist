package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.MeldingPubliserer
import no.nav.helse.kafka.messagebuilders.behovName
import no.nav.helse.kafka.messagebuilders.somJsonMessage
import no.nav.helse.mediator.KommandokjedeEndretEvent
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.melding.UtgåendeHendelse
import org.slf4j.LoggerFactory
import java.util.UUID

class MessageContextMeldingPubliserer(private val context: MessageContext) : MeldingPubliserer {
    override fun publiser(
        fødselsnummer: String,
        hendelse: UtgåendeHendelse,
        årsak: String,
    ) {
        val packet = hendelse.somJsonMessage(fødselsnummer).toJson()
        logg.info("Publiserer hendelse på grunn av $årsak")
        sikkerlogg.info("Publiserer hendelse på grunn av $årsak\n{}", packet)
        context.publish(fødselsnummer, packet)
    }

    override fun publiser(
        fødselsnummer: String,
        subsumsjonEvent: SubsumsjonEvent,
        versjonAvKode: String,
    ) {
        val packet = subsumsjonEvent.somJsonMessage(fødselsnummer, versjonAvKode).toJson()
        logg.info("Publiserer subsumsjon")
        sikkerlogg.info("Publiserer subsumsjon\n{}", packet)
        context.publish(fødselsnummer, packet)
    }

    override fun publiser(
        hendelseId: UUID,
        commandContextId: UUID,
        fødselsnummer: String,
        behov: List<Behov>,
    ) {
        val packet = behov.somJsonMessage(commandContextId, fødselsnummer, hendelseId).toJson()
        val behovNames = behov.map(Behov::behovName)
        logg.info("Publiserer behov for $behovNames")
        sikkerlogg.info("Publiserer behov for $behovNames\n{}", packet)
        context.publish(fødselsnummer, packet)
    }

    override fun publiser(
        fødselsnummer: String,
        event: KommandokjedeEndretEvent,
        hendelseNavn: String,
    ) {
        val message = JsonMessage.newMessage(event.eventName, event.detaljer()).toJson()
        logg.info(
            "Publiserer melding om tilstandsendring for kommandokjede startet av $hendelseNavn, ny tilstand: ${event::class.simpleName}",
        )
        sikkerlogg.info(
            "Publiserer melding om tilstandsendring for kommandokjede startet av $hendelseNavn, ny tilstand: ${event::class.simpleName}\n{}",
            message,
        )
        context.publish(fødselsnummer, message)
    }

    private companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}
