package no.nav.helse.modell.utbetaling

import no.nav.helse.modell.utbetaling.Utbetalingtype.REVURDERING
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Inntektskilde.EN_ARBEIDSGIVER
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Periodetype.FORLENGELSE
import no.nav.helse.modell.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING
import org.slf4j.LoggerFactory

internal class Utbetalingsfilter(
    private val fødselsnummer: String,
    private val erUtbetaltFør: Boolean,
    private val harUtbetalingTilSykmeldt: Boolean,
    private val periodetype: Periodetype,
    private val inntektskilde: Inntektskilde,
    private val utbetalingtype: Utbetalingtype,
    private val harVedtaksperiodePågåendeOverstyring: Boolean,
) {
    private val årsaker = mutableListOf<String>()
    private fun nyÅrsak(årsak: String) = årsaker.add("Brukerutbetalingsfilter: $årsak")

    private fun evaluer(): Boolean {
        if (erUtbetaltFør) return true
        if (utbetalingtype == REVURDERING) {
            sikkerLogg.info("Beholdes da det er en revurdering, fnr=$fødselsnummer")
            return true
        }
        if (!(fødselsnummer.startsWith("29") || fødselsnummer.startsWith("30") || fødselsnummer.startsWith("31"))) nyÅrsak(
            "Velges ikke ut som 'to om dagen'"
        ) // Kvoteregulering
        if (periodetype !in tillatePeriodetyper) nyÅrsak("Perioden er ikke førstegangsbehandling eller forlengelse")
        if (inntektskilde != EN_ARBEIDSGIVER) nyÅrsak("Inntektskilden er ikke for en arbeidsgiver")
        if (årsaker.isNotEmpty() && harVedtaksperiodePågåendeOverstyring) {
            sikkerLogg.info("Beholdes hos oss da det er en pågående overstyring. Årsaker registrert: ${årsaker.joinToString()}")
            return true
        }
        return årsaker.isEmpty()
    }

    internal val kanUtbetales by lazy { if (harUtbetalingTilSykmeldt) evaluer() else true }
    internal val kanIkkeUtbetales get() = !kanUtbetales

    internal fun årsaker(): List<String> {
        require(kanIkkeUtbetales) { "Årsaker skal kun brukes for vedtaksperioder vi ikke kan utbetale" }
        require(årsaker.isNotEmpty()) { "Må være minst en årsak til at vi ikke kan utbetale en vedtaksperiode" }
        return årsaker
    }

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val tillatePeriodetyper = setOf(FØRSTEGANGSBEHANDLING, FORLENGELSE)
    }
}
