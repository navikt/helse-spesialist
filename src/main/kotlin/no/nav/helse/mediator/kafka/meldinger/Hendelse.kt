package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.modell.command.nyny.Command
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import java.time.LocalDate
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
    fun godkjenning(
        message: JsonMessage,
        id: UUID,
        fødselsnummer: String,
        aktørId: String,
        organisasjonsnummer: String,
        periodeFom: LocalDate,
        periodeTom: LocalDate,
        vedtaksperiodeId: UUID,
        warnings: List<String>,
        periodetype: Saksbehandleroppgavetype?,
        context: RapidsConnection.MessageContext
    )

    fun overstyring(message: JsonMessage, id: UUID, fødselsnummer: String, context: RapidsConnection.MessageContext)
    fun tilbakerulling(
        message: JsonMessage,
        id: UUID,
        fødselsnummer: String,
        vedtaksperiodeIder: List<UUID>,
        context: RapidsConnection.MessageContext
    )
}
