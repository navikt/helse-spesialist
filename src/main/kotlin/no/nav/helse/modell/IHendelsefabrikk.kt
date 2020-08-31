package no.nav.helse.modell

import no.nav.helse.mediator.kafka.meldinger.NyVedtaksperiodeEndretMessage
import no.nav.helse.mediator.kafka.meldinger.NyVedtaksperiodeForkastetMessage
import java.util.*

internal interface IHendelsefabrikk {
    fun nyNyVedtaksperiodeEndret(id: UUID, vedtaksperiodeId: UUID, fødselsnummer: String, json: String): NyVedtaksperiodeEndretMessage
    fun nyNyVedtaksperiodeEndret(json: String): NyVedtaksperiodeEndretMessage
    fun nyNyVedtaksperiodeForkastet(id: UUID, vedtaksperiodeId: UUID, fødselsnummer: String, json: String): NyVedtaksperiodeForkastetMessage
    fun nyNyVedtaksperiodeForkastet(json: String): NyVedtaksperiodeForkastetMessage
}
