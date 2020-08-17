package no.nav.helse.mediator.kafka.meldinger

import java.util.*

internal interface Hendelse {
    val id: UUID

    fun håndter(mediator: ICommandMediator)

    fun toJson(): String
}

internal interface ICommandMediator {
    fun håndter(vedtaksperiodeEndretMessage: NyVedtaksperiodeEndretMessage)
}
