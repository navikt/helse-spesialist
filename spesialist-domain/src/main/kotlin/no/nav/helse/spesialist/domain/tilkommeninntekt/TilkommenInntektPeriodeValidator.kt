package no.nav.helse.spesialist.domain.tilkommeninntekt

import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.spesialist.domain.Periode

object TilkommenInntektPeriodeValidator {
    fun validerPeriode(
        periode: Periode,
        organisasjonsnummer: String,
        andreTilkomneInntekter: List<TilkommenInntekt>,
        vedtaksperioder: List<VedtaksperiodeDto>,
    ) {
        if (!erInnenforEtSykefraværstilfelle(periode = periode, vedtaksperioder = vedtaksperioder)) {
            error("Kan ikke legge til tilkommen inntekt som går utenfor et sykefraværstilfelle")
        }
        validerAtNyPeriodeIkkeOverlapperEksisterendePerioder(
            periode = periode,
            organisasjonsnummer = organisasjonsnummer,
            andreTilkomneInntekter = andreTilkomneInntekter,
        )
    }

    fun validerAtNyPeriodeIkkeOverlapperEksisterendePerioder(
        periode: Periode,
        organisasjonsnummer: String,
        andreTilkomneInntekter: List<TilkommenInntekt>,
    ) {
        val andreTilkomneInntekterForInntektskilde =
            andreTilkomneInntekter.filter { it.organisasjonsnummer == organisasjonsnummer }
        if (andreTilkomneInntekterForInntektskilde.any { it.periode overlapper periode }) {
            error("Kan ikke legge til tilkommen inntekt som overlapper med en annen tilkommen inntekt for samme inntektskilde")
        }
    }

    fun erInnenforEtSykefraværstilfelle(
        periode: Periode,
        vedtaksperioder: List<VedtaksperiodeDto>,
    ) = periode erInnenforEnAv vedtaksperioder.tilSykefraværstillfellePerioder().filterNot { it.datoer().isEmpty() }

    private fun List<VedtaksperiodeDto>.tilSykefraværstillfellePerioder(): List<Periode> =
        map { it.behandlinger.last() }
            .groupBy(BehandlingDto::skjæringstidspunkt, BehandlingDto::tom)
            .map { (skjæringstidspunkt, listeAvTom) -> Periode(fom = skjæringstidspunkt, tom = listeAvTom.max()) }
}
