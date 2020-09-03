package no.nav.helse.modell

import no.nav.helse.mediator.kafka.meldinger.NyGodkjenningMessage
import no.nav.helse.mediator.kafka.meldinger.NyVedtaksperiodeEndretMessage
import no.nav.helse.mediator.kafka.meldinger.NyVedtaksperiodeForkastetMessage
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import java.time.LocalDate
import java.util.*

internal interface IHendelsefabrikk {
    fun nyNyVedtaksperiodeEndret(id: UUID, vedtaksperiodeId: UUID, fødselsnummer: String, json: String): NyVedtaksperiodeEndretMessage
    fun nyNyVedtaksperiodeEndret(json: String): NyVedtaksperiodeEndretMessage
    fun nyNyVedtaksperiodeForkastet(id: UUID, vedtaksperiodeId: UUID, fødselsnummer: String, json: String): NyVedtaksperiodeForkastetMessage
    fun nyNyVedtaksperiodeForkastet(json: String): NyVedtaksperiodeForkastetMessage
    fun nyGodkjenning(
        id: UUID,
        fødselsnummer: String,
        aktørId: String,
        organisasjonsnummer: String,
        periodeFom: LocalDate,
        periodeTom: LocalDate,
        vedtaksperiodeId: UUID,
        warnings: List<String>,
        periodetype: Saksbehandleroppgavetype?,
        json: String
    ): NyGodkjenningMessage
    fun nyGodkjenning(json: String): NyGodkjenningMessage
}
