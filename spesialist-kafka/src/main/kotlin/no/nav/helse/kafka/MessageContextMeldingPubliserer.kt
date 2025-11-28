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
import no.nav.helse.spesialist.application.logg.loggInfo
import java.util.UUID

class MessageContextMeldingPubliserer(
    private val context: MessageContext,
) : MeldingPubliserer {
    override fun publiser(
        fødselsnummer: String,
        hendelse: UtgåendeHendelse,
        årsak: String,
    ) {
        val packet = hendelse.somJsonMessage(fødselsnummer).toJson()
        loggInfo("Publiserer hendelse på grunn av $årsak", "json:\n$packet")
        context.publish(fødselsnummer, packet)
    }

    override fun publiser(
        fødselsnummer: String,
        subsumsjonEvent: SubsumsjonEvent,
        versjonAvKode: String,
    ) {
        val packet = subsumsjonEvent.somJsonMessage(fødselsnummer, versjonAvKode).toJson()
        loggInfo("Publiserer subsumsjon", "json:\n$packet")
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
        loggInfo("Publiserer behov for $behovNames", "json:\n$packet")
        context.publish(fødselsnummer, packet)
    }

    override fun publiser(
        fødselsnummer: String,
        event: KommandokjedeEndretEvent,
        hendelseNavn: String,
    ) {
        val message = JsonMessage.newMessage(event.eventName, event.detaljer()).toJson()
        loggInfo("Publiserer melding om tilstandsendring for kommandokjede startet av $hendelseNavn, ny tilstand: ${event::class.simpleName}", "json:\n$message")
        context.publish(fødselsnummer, message)
    }
}
