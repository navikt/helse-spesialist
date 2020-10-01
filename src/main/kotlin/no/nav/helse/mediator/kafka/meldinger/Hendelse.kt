package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.modell.command.nyny.Command
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.LocalDateTime
import java.util.*

internal interface Hendelse : Command {
    val id: UUID

    fun fødselsnummer(): String
    fun vedtaksperiodeId(): UUID? = null

    fun toJson(): String
}

internal interface IHendelseMediator {
    fun vedtaksperiodeEndret(
        message: JsonMessage,
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        context: RapidsConnection.MessageContext
    )

    fun vedtaksperiodeForkastet(
        message: JsonMessage,
        id: UUID,
        vedtaksperiodeId: UUID,
        fødselsnummer: String,
        context: RapidsConnection.MessageContext
    )

    fun løsning(hendelseId: UUID, contextId: UUID, løsning: Any, context: RapidsConnection.MessageContext)

    fun standardfelter(hendelsetype: String, fødselsnummer: String) = mutableMapOf(
        "@event_name" to hendelsetype,
        "@opprettet" to LocalDateTime.now(),
        "@id" to UUID.randomUUID(),
        "fødselsnummer" to fødselsnummer
    )
}
