package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.modell.command.nyny.CommandContext
import no.nav.helse.rapids_rivers.JsonMessage
import java.util.*

internal interface Hendelse {
    val id: UUID

    fun håndter(mediator: ICommandMediator, context: CommandContext)

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
    fun håndter(message: JsonMessage, hendelse: Hendelse)
}

internal interface ICommandMediator {
    fun håndter(vedtaksperiodeEndretMessage: NyVedtaksperiodeEndretMessage, context: CommandContext)
    fun håndter(vedtaksperiodeForkastetMessage: NyVedtaksperiodeForkastetMessage, context: CommandContext)
}
