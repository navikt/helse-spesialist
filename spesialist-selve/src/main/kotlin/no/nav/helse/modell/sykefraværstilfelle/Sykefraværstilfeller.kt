package no.nav.helse.modell.sykefraværstilfelle

import java.util.UUID
import no.nav.helse.mediator.meldinger.Personhendelse
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeOppdatering

internal class Sykefraværstilfeller(
    override val id: UUID,
    private val fødselsnummer: String,
    val aktørId: String,
    val vedtaksperiodeOppdateringer: List<VedtaksperiodeOppdatering>,
    private val json: String,
) : Personhendelse {
    override fun fødselsnummer() = fødselsnummer
    override fun toJson() = json
}