package no.nav.helse.spesialist.domain.tilkommeninntekt

import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Behandling.Companion.tilSykefraværstilfellePerioder
import no.nav.helse.spesialist.domain.Periode

object TilkommenInntektPeriodeValidator {
    fun validerPeriode(
        periode: Periode,
        organisasjonsnummer: String,
        andreTilkomneInntekter: List<TilkommenInntekt>,
        behandlinger: List<Behandling>,
    ) {
        if (!erInnenforEtSykefraværstilfelle(periode = periode, behandlinger = behandlinger)) {
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
        behandlinger: List<Behandling>,
    ) = periode erInnenforEnAv behandlinger.tilSykefraværstilfellePerioder().filterNot { it.datoer().isEmpty() }
}
