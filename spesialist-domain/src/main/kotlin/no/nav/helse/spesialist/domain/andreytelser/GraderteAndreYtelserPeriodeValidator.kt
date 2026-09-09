package no.nav.helse.spesialist.domain.andreytelser

import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Behandling.Companion.tilSykefraværstilfellePerioder
import no.nav.helse.spesialist.domain.andreytelser.AndreYtelserPeriode.GraderteAndreYtelserPeriode

fun validerGraderteAndreYtelserPeriode(
    nyGraderteAndreYtelserPerioder: List<GraderteAndreYtelserPeriode>,
    nyGraderteAndreYtelserType: GraderteAndreYtelserType,
    eksisterendeGraderteAndreYtelser: List<GraderteAndreYtelser>,
    behandlinger: List<Behandling>,
) {
    val sortertePerioder = nyGraderteAndreYtelserPerioder.map { it.periode }.sortedBy { it.fom }
    if (sortertePerioder.zipWithNext().any { (a, b) -> b.fom <= a.tom }) {
        error("Perioder kan ikke overlappe hverandre")
    }
    if (!harOverlappMedSykefraværstilfelle(
            nyGraderteAndreYtelserPerioder = nyGraderteAndreYtelserPerioder,
            behandlinger = behandlinger,
        )
    ) {
        error("Ingen sykefraværstilfeller overlapper med perioden(e) i gradert annen ytelse ($nyGraderteAndreYtelserPerioder)")
    }

    validerAtTotalGraderingIkkeOverstiger100Prosent(eksisterendeGraderteAndreYtelser, nyGraderteAndreYtelserPerioder)
    validerAtNyPeriodeIkkeOverlapperEksisterendePerioder(
        nyGraderteAndreYtelserPerioder = nyGraderteAndreYtelserPerioder,
        graderteAndreYtelserType = nyGraderteAndreYtelserType,
        eksisterendeGraderteAndreYtelser = eksisterendeGraderteAndreYtelser,
    )
}

private fun validerAtTotalGraderingIkkeOverstiger100Prosent(
    eksisterendeGraderteAndreYtelser: List<GraderteAndreYtelser>,
    nyGraderteAndreYtelserPerioder: List<GraderteAndreYtelserPeriode>,
) {
    val allePerioder = eksisterendeGraderteAndreYtelser.flatMap { it.perioder } + nyGraderteAndreYtelserPerioder
    val tidslinjeMapMedGrad =
        allePerioder
            .flatMap { periode ->
                periode.periode.datoer().map { dato ->
                    Pair(dato, periode.grad)
                }
            }.groupBy({ it.first }, { it.second })
    val erDetOver99Prosent = tidslinjeMapMedGrad.map { (_, grader) -> grader.sum() }.any { it > 99 }

    if (erDetOver99Prosent) {
        error("Minst en dag overskrider 99 prosent på tvers av graderte andre ytelser")
    }
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
    behandlinger: List<Behandling>,
) = nyGraderteAndreYtelserPerioder.all {
    it.periode overlapperEnAv behandlinger.tilSykefraværstilfellePerioder().filterNot { it.datoer().isEmpty() }
}
