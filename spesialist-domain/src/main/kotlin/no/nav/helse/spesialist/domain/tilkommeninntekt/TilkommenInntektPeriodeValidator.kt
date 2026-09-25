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
    ): Resultat {
        if (!erInnenforEtSykefraværstilfelle(periode = periode, behandlinger = behandlinger)) {
            return Resultat.GårUtenforSykefraværstilfelle
        }
        return validerAtNyPeriodeIkkeOverlapperEksisterendePerioder(
            periode = periode,
            organisasjonsnummer = organisasjonsnummer,
            andreTilkomneInntekter = andreTilkomneInntekter,
        )
    }

    fun validerAtNyPeriodeIkkeOverlapperEksisterendePerioder(
        periode: Periode,
        organisasjonsnummer: String,
        andreTilkomneInntekter: List<TilkommenInntekt>,
    ): Resultat {
        val andreTilkomneInntekterForInntektskilde =
            andreTilkomneInntekter.filter { it.organisasjonsnummer == organisasjonsnummer }
        if (andreTilkomneInntekterForInntektskilde.any { it.periode overlapper periode }) {
            return Resultat.OverlapperAnnenTilkommenInntekt
        }
        return Resultat.OK
    }

    fun erInnenforEtSykefraværstilfelle(
        periode: Periode,
        behandlinger: List<Behandling>,
    ) = periode erInnenforEnAv behandlinger.tilSykefraværstilfellePerioder().filterNot { it.datoer().isEmpty() }

    sealed interface Resultat {
        object OK : Resultat

        object GårUtenforSykefraværstilfelle : Resultat

        object OverlapperAnnenTilkommenInntekt : Resultat
    }
}
