package no.nav.helse.spesialist.domain.tilkommeninntekt

import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.spesialist.domain.Periode
import org.slf4j.Logger
import java.util.UUID

object TilkommenInntektPeriodeValidator {
    fun validerPeriode(
        periode: Periode,
        organisasjonsnummer: String,
        andreTilkomneInntekter: List<TilkommenInntekt>,
        vedtaksperioder: List<VedtaksperiodeDto>,
        sikkerlogg: Logger,
    ) {
        if (!erInnenforEtSykefraværstilfelle(periode = periode, vedtaksperioder = vedtaksperioder, sikkerlogg = sikkerlogg)) {
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
        sikkerlogg: Logger? = null,
    ): Boolean {
        val sykefraværstilfellePerioder = vedtaksperioder.tilSykefraværstillfellePerioder()
        val erInnenforEnPeriode =
            periode erInnenforEnAv (
                sykefraværstilfellePerioder
                    .map { it.second }
                    .filterNot { it.datoer().isEmpty() }
            )
        return if (erInnenforEnPeriode) {
            true
        } else {
            false.also { sikkerlogg?.info("Periode $periode overlapper ikke med noen sykefraværstilfelleperioder $sykefraværstilfellePerioder") }
        }
    }

    fun List<VedtaksperiodeDto>.tilSykefraværstillfellePerioder(): List<Pair<UUID, Periode>> =
        map { it.behandlinger.last() }
            .map { it.id to Periode(it.fom, it.tom) }
            .sortedBy { it.second.fom }
            .fold(listOf()) { sammenhengendePerioder, (id, nestePeriode) ->
                val (overlappendePerioder, resten) =
                    sammenhengendePerioder.partition {
                        nestePeriode overlapper Periode(it.second.fom, it.second.tom.plusDays(1))
                    }
                if (overlappendePerioder.isEmpty()) {
                    resten + (id to nestePeriode)
                } else {
                    resten + overlappendePerioder.first().let { it.first to it.second.copy(tom = maxOf(it.second.tom, nestePeriode.tom)) }
                }
            }
}
