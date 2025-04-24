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
        verifiserAtErInnenforEtSykefraværstilfelle(
            periode = periode,
            vedtaksperioder = vedtaksperioder,
        )
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

    fun verifiserAtErInnenforEtSykefraværstilfelle(
        periode: Periode,
        vedtaksperioder: List<VedtaksperiodeDto>,
    ) {
        val sykefraværstilfellePerioder =
            vedtaksperioder.tilSykefraværstillfellePerioder()
                .map { it.utenFørsteDag() }
                .filterNot { it.datoer().isEmpty() }

        if (!(periode erInnenforEnAv sykefraværstilfellePerioder)) {
            error("Kan ikke legge til tilkommen inntekt som går utenfor et sykefraværstilfelle")
        }
    }

    private fun List<VedtaksperiodeDto>.tilSykefraværstillfellePerioder(): List<Periode> {
        return map { it.behandlinger.last() }
            .groupBy(BehandlingDto::skjæringstidspunkt, BehandlingDto::tom)
            .map { (skjæringstidspunkt, listeAvTom) -> Periode(fom = skjæringstidspunkt, tom = listeAvTom.max()) }
    }

    private fun Periode.utenFørsteDag() = copy(fom = fom.plusDays(1))
}
