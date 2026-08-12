package no.nav.helse.spesialist.domain.andreytelser

import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.andreytelser.AndreYtelserPeriode.GraderteAndreYtelserPeriode

fun validerGraderteAndreYtelserPeriode(
    nyGraderteAndreYtelserPerioder: List<GraderteAndreYtelserPeriode>,
    nyGraderteAndreYtelserType: GraderteAndreYtelserType,
    eksisterendeGraderteAndreYtelser: List<GraderteAndreYtelser>,
    vedtaksperioder: List<VedtaksperiodeDto>,
) {
    val sortertePerioder = nyGraderteAndreYtelserPerioder.map { it.periode }.sortedBy { it.fom }
    if (sortertePerioder.zipWithNext().any { (a, b) -> b.fom <= a.tom }) {
        error("Perioder kan ikke overlappe hverandre")
    }
    if (!harOverlappMedSykefraværstilfelle(
            nyGraderteAndreYtelserPerioder = nyGraderteAndreYtelserPerioder,
            vedtaksperioder = vedtaksperioder,
        )
    ) {
        error("Kan ikke legge til andre ytelser som ikke overlapper med et sykefraværstilfelle")
    }
    validerAtNyPeriodeIkkeOverlapperEksisterendePerioder(
        nyGraderteAndreYtelserPerioder = nyGraderteAndreYtelserPerioder,
        graderteAndreYtelserType = nyGraderteAndreYtelserType,
        eksisterendeGraderteAndreYtelser = eksisterendeGraderteAndreYtelser,
    )
}

private fun validerAtNyPeriodeIkkeOverlapperEksisterendePerioder(
    nyGraderteAndreYtelserPerioder: List<GraderteAndreYtelserPeriode>,
    graderteAndreYtelserType: GraderteAndreYtelserType,
    eksisterendeGraderteAndreYtelser: List<GraderteAndreYtelser>,
) {
    val eksisterendeAndreYtelserPerioder =
        eksisterendeGraderteAndreYtelser
            .filter { it.graderteAndreYtelserType === graderteAndreYtelserType }
            .flatMap { eksisterendeAndreYtelse ->
                eksisterendeAndreYtelse.perioder.map { andreYtelserPeriode ->
                    andreYtelserPeriode.periode
                }
            }

    if (nyGraderteAndreYtelserPerioder.any {
            it.periode overlapperEnAv eksisterendeAndreYtelserPerioder
        }
    ) {
        error("Kan ikke legge til andre ytelser som overlapper med periode som har samme type")
    }
}

private fun harOverlappMedSykefraværstilfelle(
    nyGraderteAndreYtelserPerioder: List<GraderteAndreYtelserPeriode>,
    vedtaksperioder: List<VedtaksperiodeDto>,
) = nyGraderteAndreYtelserPerioder.all {
    it.periode overlapperEnAv vedtaksperioder.tilSykefraværstilfellePerioder().filterNot { it.datoer().isEmpty() }
}

private fun List<VedtaksperiodeDto>.tilSykefraværstilfellePerioder(): List<Periode> =
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
