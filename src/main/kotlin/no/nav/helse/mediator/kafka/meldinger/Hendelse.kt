package no.nav.helse.mediator.kafka.meldinger

internal interface Hendelse {
    fun håndter(mediator: ICommandMediator)
}

internal interface ICommandMediator {
    fun håndter(vedtaksperiodeEndretMessage: NyVedtaksperiodeEndretMessage)
}
