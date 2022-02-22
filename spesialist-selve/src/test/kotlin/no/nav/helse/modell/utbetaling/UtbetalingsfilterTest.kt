package no.nav.helse.modell.utbetaling

import no.nav.helse.modell.utbetaling.Utbetalingtype.REVURDERING
import no.nav.helse.modell.utbetaling.Utbetalingtype.UTBETALING
import no.nav.helse.modell.vedtak.Warning
import no.nav.helse.modell.vedtak.WarningKilde
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Inntektskilde.EN_ARBEIDSGIVER
import no.nav.helse.modell.vedtaksperiode.Inntektskilde.FLERE_ARBEIDSGIVERE
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Periodetype.FORLENGELSE
import no.nav.helse.modell.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.modell.vedtaksperiode.Periodetype.INFOTRYGDFORLENGELSE
import no.nav.helse.modell.vedtaksperiode.Periodetype.OVERGANG_FRA_IT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class UtbetalingsfilterTest {

    @Test
    fun `ingen utbetaling kan utbetales`() {
        assertKanUtbetales(Utbetalingsfilter(
            fødselsnummer = "21111111111",
            utbetalingTilArbeidsgiver = false,
            utbetalingTilSykmeldt = false,
            periodetype = OVERGANG_FRA_IT,
            warnings = enWarning,
            inntektskilde = FLERE_ARBEIDSGIVERE,
            utbetalingtype = UTBETALING
        ))
    }

    @Test
    fun `full refusjon kan utbetales`() {
        assertKanUtbetales(Utbetalingsfilter(
            fødselsnummer = "21111111111",
            utbetalingTilArbeidsgiver = true,
            utbetalingTilSykmeldt = false,
            periodetype = OVERGANG_FRA_IT,
            warnings = enWarning,
            inntektskilde = FLERE_ARBEIDSGIVERE,
            utbetalingtype = UTBETALING
        ))
    }

    @Test
    fun `delvis refusjon kan ikke utbetales`() {
        assertKanIkkeUtbetales(utbetalingsfilter(utbetalingTilArbeidsgiver = true, utbetalingTilSykmeldt = true), listOf("Utbetalingsfilter: Utbetalingen består av delvis refusjon"))
    }

    @Test
    fun `ingen refusjon kan utbetales`() {
        assertKanUtbetales(utbetalingsfilter())
    }

    @Test
    fun `ingen refusjon & feil periodetype kan ikke utbetales`() {
        assertKanIkkeUtbetales(utbetalingsfilter(periodetype = INFOTRYGDFORLENGELSE), listOf("Utbetalingsfilter: Perioden er ikke førstegangsbehandling eller forlengelse"))
    }

    @Test
    fun `ingen refusjon & spleis forlengelse kan utbetales`() {
        assertKanUtbetales(utbetalingsfilter(periodetype = FORLENGELSE))
    }

    @Test
    fun `ingen refusjon & feil fødselsdato kan ikke utbetales`() {
        assertKanIkkeUtbetales(utbetalingsfilter(fødselsnummer = "21111111111"), listOf("Utbetalingsfilter: Fødselsdag passer ikke"))
    }

    @Test
    fun `ingen refusjon & inntektskilde fra flere arbeidsgivere kan ikke utbetales`() {
        assertKanIkkeUtbetales(utbetalingsfilter(inntektskilde = FLERE_ARBEIDSGIVERE), listOf("Utbetalingsfilter: Inntektskilden er ikke for en arbeidsgiver"))
    }

    @Test
    fun `ingen refusjon & warnings på vedtaksperiode kan ikke utbetales`() {
        assertKanIkkeUtbetales(utbetalingsfilter(warnings = enWarning), listOf("Utbetalingsfilter: Vedtaksperioden har warnings"))
    }

    @Test
    fun `revurdering kan utbetales tross warnings`() {
        assertKanUtbetales(utbetalingsfilter(utbetalingstype = REVURDERING, warnings = enWarning))
    }

    @Test
    fun `passerer ingen av kriteriene i filteret`() {
        assertKanIkkeUtbetales(Utbetalingsfilter(
            fødselsnummer = "21111111111",
            utbetalingTilArbeidsgiver = true,
            utbetalingTilSykmeldt = true,
            periodetype = OVERGANG_FRA_IT,
            warnings = enWarning,
            inntektskilde = FLERE_ARBEIDSGIVERE,
            utbetalingtype = UTBETALING
        ), listOf(
            "Utbetalingsfilter: Utbetalingen består av delvis refusjon",
            "Utbetalingsfilter: Fødselsdag passer ikke",
            "Utbetalingsfilter: Perioden er ikke førstegangsbehandling eller forlengelse",
            "Utbetalingsfilter: Inntektskilden er ikke for en arbeidsgiver",
            "Utbetalingsfilter: Vedtaksperioden har warnings"
        ))
    }

    private companion object {
        private val enWarning = listOf(Warning("testwarning", WarningKilde.Spesialist))

        private fun utbetalingsfilter(
            fødselsnummer: String = "31111111111",
            utbetalingTilArbeidsgiver: Boolean = false,
            utbetalingTilSykmeldt: Boolean = true,
            periodetype: Periodetype = FØRSTEGANGSBEHANDLING,
            inntektskilde: Inntektskilde = EN_ARBEIDSGIVER,
            warnings: List<Warning> = emptyList(),
            utbetalingstype: Utbetalingtype = UTBETALING
        ) = Utbetalingsfilter(
            fødselsnummer = fødselsnummer,
            utbetalingTilArbeidsgiver = utbetalingTilArbeidsgiver,
            utbetalingTilSykmeldt = utbetalingTilSykmeldt,
            periodetype = periodetype,
            warnings = warnings,
            inntektskilde = inntektskilde,
            utbetalingtype = utbetalingstype
        )

        private fun assertKanUtbetales(filter: Utbetalingsfilter) {
            assertTrue(filter.kanUtbetales)
            assertFalse(filter.kanIkkeUtbetales)
            assertThrows<IllegalArgumentException> { filter.årsaker() }
        }

        private fun assertKanIkkeUtbetales(filter: Utbetalingsfilter, forventedeÅrsaker: List<String>) {
            assertFalse(filter.kanUtbetales)
            assertTrue(filter.kanIkkeUtbetales)
            assertEquals(filter.årsaker(), forventedeÅrsaker)
        }
    }
}
