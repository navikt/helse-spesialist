package no.nav.helse.mediator.meldinger.hendelser

import java.util.UUID
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.vedtaksperiode.vedtak.AvsluttetUtenVedtak
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate

internal class AvsluttetUtenVedtakMessage(packet: JsonMessage) {

    private val fødselsnummer = packet["fødselsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()
    private val fom = packet["fom"].asLocalDate()
    private val tom = packet["tom"].asLocalDate()
    private val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate()
    private val hendelser = packet["hendelser"].map { it.asUUID() }

    internal fun skjæringstidspunkt() = skjæringstidspunkt
    internal fun fødselsnummer() = fødselsnummer

    private val avsluttetUtenVedtak get() = AvsluttetUtenVedtak(
        fødselsnummer = fødselsnummer,
        aktørId = aktørId,
        organisasjonsnummer = organisasjonsnummer,
        vedtaksperiodeId = vedtaksperiodeId,
        skjæringstidspunkt = skjæringstidspunkt,
        hendelser = hendelser,
        fom = fom,
        tom = tom,
    )

    internal fun sendInnTil(sykefraværstilfelle: Sykefraværstilfelle) {
        sykefraværstilfelle.håndter(avsluttetUtenVedtak)
    }

}
