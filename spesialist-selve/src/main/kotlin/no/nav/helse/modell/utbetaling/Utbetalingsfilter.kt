package no.nav.helse.modell.utbetaling

import no.nav.helse.modell.utbetaling.Utbetalingtype.REVURDERING
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Inntektskilde.EN_ARBEIDSGIVER
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Periodetype.FORLENGELSE
import no.nav.helse.modell.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING

internal class Utbetalingsfilter(
    private val fødselsnummer: String,
    private val utbetalingTilArbeidsgiver: Boolean,
    private val utbetalingTilSykmeldt: Boolean,
    private val periodetype: Periodetype,
    private val inntektskilde: Inntektskilde,
    private val warnings: List<Warning>,
    private val utbetalingtype: Utbetalingtype) {

    private val årsaker = mutableListOf<String>()
    private fun nyÅrsak(årsak: String) = årsaker.add("Utbetalingsfilter: $årsak")

    private fun evaluer(): Boolean{
        if (!utbetalingTilSykmeldt) return true // Full refusjon / ingen utbetaling kan alltid utbetales
        if (utbetalingTilArbeidsgiver && utbetalingTilSykmeldt) nyÅrsak("Utbetalingen består av delvis refusjon")
        if (!fødselsnummer.startsWith("31")) nyÅrsak("Fødselsdag passer ikke")
        if (periodetype !in tillatePeriodetyper) nyÅrsak("Perioden er ikke førstegangsbehandling eller forlengelse")
        if (inntektskilde != EN_ARBEIDSGIVER) nyÅrsak("Inntektskilden er ikke for en arbeidsgiver")
        // Unngå ping-pong om en av de utvalgte utbetalingene til sykmeldt revurderes og får warning
        if (warnings.isNotEmpty() && utbetalingtype != REVURDERING) nyÅrsak("Vedtaksperioden har warnings")
        return årsaker.isEmpty()
    }

    internal val kanUtbetales by lazy {evaluer()}
    internal val kanIkkeUtbetales get() = !kanUtbetales

    internal fun årsaker(): List<String> {
        require(kanIkkeUtbetales) { "Årsaker skal kun brukes for vedtaksperioder vi ikke kan utbetale" }
        require(årsaker.isNotEmpty()) { "Må være minst en årsak til at vi ikke kan utbetale en vedtaksperiode" }
        return årsaker
    }

    private companion object {
        private val tillatePeriodetyper = setOf(FØRSTEGANGSBEHANDLING, FORLENGELSE)
    }
}
