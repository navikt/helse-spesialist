package no.nav.helse.spesialist.domain.tilkommeninntekt

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

    fun List<VedtaksperiodeDto>.tilSykefraværstillfellePerioder(): List<Periode> =
        map { it.behandlinger.last() }
            .map { Periode(it.fom, it.tom) }
            .sortedBy { it.fom }
            .fold(listOf()) { sammenhengendePerioder, nestePeriode ->
                val (overlappendePerioder, resten) =
                    sammenhengendePerioder.partition {
                        nestePeriode overlapper Periode(it.fom, it.tom.plusDays(1))
                    }
                if (overlappendePerioder.isEmpty()) {
                    resten + nestePeriode
                } else {
                    resten + overlappendePerioder.first().let { it.copy(tom = maxOf(it.tom, nestePeriode.tom)) }
                }
            }
}
