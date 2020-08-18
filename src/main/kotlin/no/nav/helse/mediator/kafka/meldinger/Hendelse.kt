package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.modell.command.nyny.CommandContext
import java.util.*

internal interface Hendelse {
    val id: UUID

    fun håndter(mediator: ICommandMediator, context: CommandContext)

    fun toJson(): String
}

internal interface IHendelseMediator {
    fun håndter(hendelse: Hendelse)
}

internal interface ICommandMediator {
    fun håndter(vedtaksperiodeEndretMessage: NyVedtaksperiodeEndretMessage, context: CommandContext)
}
