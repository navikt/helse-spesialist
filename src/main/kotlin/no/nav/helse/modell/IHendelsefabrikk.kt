package no.nav.helse.modell

import no.nav.helse.mediator.kafka.meldinger.NyVedtaksperiodeEndretMessage
import no.nav.helse.mediator.kafka.meldinger.NyVedtaksperiodeForkastetMessage

internal interface IHendelsefabrikk {
    fun nyNyVedtaksperiodeEndret(json: String): NyVedtaksperiodeEndretMessage
    fun nyNyVedtaksperiodeForkastet(json: String): NyVedtaksperiodeForkastetMessage
}
