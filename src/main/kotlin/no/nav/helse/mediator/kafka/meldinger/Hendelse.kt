package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.modell.command.nyny.Command
import no.nav.helse.modell.command.nyny.CommandContext
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.*

internal interface Hendelse : Command {
    val id: UUID

    fun fødselsnummer(): String
    fun vedtaksperiodeId(): UUID

    fun toJson(): String
}

internal interface Delløsning {
    val behovId: UUID
    val contextId: UUID

    fun context(context: CommandContext) {
        context.add(this)
    }
}

internal interface IHendelseMediator {
    fun vedtaksperiodeEndret(message: JsonMessage, id: UUID, vedtaksperiodeId: UUID, fødselsnummer: String, context: RapidsConnection.MessageContext)
    fun vedtaksperiodeForkastet(message: JsonMessage, id: UUID, vedtaksperiodeId: UUID, fødselsnummer: String, context: RapidsConnection.MessageContext)
    fun løsning(hendelseId: UUID, contextId: UUID, løsning: Any, context: RapidsConnection.MessageContext)
}
