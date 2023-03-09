package no.nav.helse.modell.utbetaling

import java.time.LocalDateTime
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
        assertKanUtbetales(
            Utbetalingsfilter(
                fødselsnummer = "21111111111",
                delvisRefusjon = false,
                erUtbetaltFør = false,
                harUtbetalingTilSykmeldt = false,
                periodetype = OVERGANG_FRA_IT,
                inntektskilde = FLERE_ARBEIDSGIVERE,
                warnings = toWarnings,
                utbetalingtype = UTBETALING,
                harVedtaksperiodePågåendeOverstyring = false,
            )
        )
    }

    @Test
    fun `ny, med full refusjon, kan utbetales`() {
        assertKanUtbetales(
            Utbetalingsfilter(
                fødselsnummer = "21111111111",
                delvisRefusjon = false,
                erUtbetaltFør = false,
                harUtbetalingTilSykmeldt = false,
                periodetype = OVERGANG_FRA_IT,
                inntektskilde = FLERE_ARBEIDSGIVERE,
                warnings = toWarnings,
                utbetalingtype = UTBETALING,
                harVedtaksperiodePågåendeOverstyring = false,
            )
        )
    }

    @Test
    fun `endring, med full refusjon, kan utbetales`() {
        assertKanUtbetales(
            Utbetalingsfilter(
                fødselsnummer = "21111111111",
                delvisRefusjon = false,
                erUtbetaltFør = true,
                harUtbetalingTilSykmeldt = false,
                periodetype = OVERGANG_FRA_IT,
                inntektskilde = FLERE_ARBEIDSGIVERE,
                warnings = toWarnings,
                utbetalingtype = UTBETALING,
                harVedtaksperiodePågåendeOverstyring = false,
            )
        )
    }

    @Test
    fun `endring, endring fra full refusjon til delvis, kan utbetales`() {
        fun utbetalingsfilter() = Utbetalingsfilter(
            fødselsnummer = "21111111111",
            delvisRefusjon = true,
            erUtbetaltFør = true,
            harUtbetalingTilSykmeldt = true,
            periodetype = FORLENGELSE,
            inntektskilde = EN_ARBEIDSGIVER,
            warnings = emptyList(),
            utbetalingtype = UTBETALING,
            harVedtaksperiodePågåendeOverstyring = false,
        )
        assertKanUtbetales(utbetalingsfilter())
    }

    @Test
    fun `delvis refusjon kan ikke utbetales`() {
        assertKanIkkeUtbetales(
            utbetalingsfilter(delvisRefusjon = true),
            listOf("Brukerutbetalingsfilter: Utbetalingen består av delvis refusjon")
        )
    }

    @Test
    fun `delvis refusjon kan utbetales dersom vedtaksperioden har pågående overstyring`() {
        assertKanUtbetales(
            utbetalingsfilter(delvisRefusjon = true, harVedtaksperiodePågåendeOverstyring = true)
        )
    }

    @Test
    fun `ingen refusjon kan utbetales`() {
        assertKanUtbetales(utbetalingsfilter(harUtbetalingTilSykmeldt = true))
    }

    @Test
    fun `ingen refusjon & feil periodetype kan ikke utbetales`() {
        assertKanIkkeUtbetales(
            utbetalingsfilter(periodetype = INFOTRYGDFORLENGELSE),
            listOf("Brukerutbetalingsfilter: Perioden er ikke førstegangsbehandling eller forlengelse")
        )
    }

    @Test
    fun `ingen refusjon & spleis forlengelse kan utbetales`() {
        assertKanUtbetales(utbetalingsfilter(periodetype = FORLENGELSE))
    }

    @Test
    fun `ingen refusjon & feil fødselsdato kan ikke utbetales`() {
        assertKanIkkeUtbetales(
            utbetalingsfilter(fødselsnummer = "21111111111"),
            listOf("Brukerutbetalingsfilter: Velges ikke ut som 'to om dagen'")
        )
    }

    @Test
    fun `ingen refusjon & inntektskilde fra flere arbeidsgivere kan ikke utbetales`() {
        assertKanIkkeUtbetales(
            utbetalingsfilter(inntektskilde = FLERE_ARBEIDSGIVERE),
            listOf("Brukerutbetalingsfilter: Inntektskilden er ikke for en arbeidsgiver")
        )
    }

    @Test
    fun `ingen refusjon & warnings på vedtaksperiode kan ikke utbetales`() {
        assertKanIkkeUtbetales(
            utbetalingsfilter(warnings = toWarnings),
            listOf("Brukerutbetalingsfilter: Vedtaksperioden har warnings")
        )
    }

    @Test
    fun `revurdering kan utbetales tross warnings`() {
        assertKanUtbetales(utbetalingsfilter(utbetalingstype = REVURDERING, warnings = toWarnings))
    }

    @Test
    fun `passerer ingen av kriteriene i filteret`() {
        assertKanIkkeUtbetales(
            Utbetalingsfilter(
                fødselsnummer = "21111111111",
                delvisRefusjon = true,
                erUtbetaltFør = false,
                harUtbetalingTilSykmeldt = true,
                periodetype = OVERGANG_FRA_IT,
                inntektskilde = FLERE_ARBEIDSGIVERE,
                warnings = toWarnings,
                utbetalingtype = UTBETALING,
                harVedtaksperiodePågåendeOverstyring = false,
            ), listOf(
                "Brukerutbetalingsfilter: Utbetalingen består av delvis refusjon",
                "Brukerutbetalingsfilter: Velges ikke ut som 'to om dagen'",
                "Brukerutbetalingsfilter: Perioden er ikke førstegangsbehandling eller forlengelse",
                "Brukerutbetalingsfilter: Inntektskilden er ikke for en arbeidsgiver",
                "Brukerutbetalingsfilter: Vedtaksperioden har warnings"
            )
        )
    }

    private companion object {
        private val toWarnings = listOf(
            Warning("En warning fra Spesialist", WarningKilde.Spesialist, LocalDateTime.now()),
            Warning("En warning fra Spleis", WarningKilde.Spleis, LocalDateTime.now())
        )

        private fun utbetalingsfilter(
            fødselsnummer: String = "31111111111",
            delvisRefusjon: Boolean = false,
            harUtbetalingTilSykmeldt: Boolean = true,
            periodetype: Periodetype = FØRSTEGANGSBEHANDLING,
            inntektskilde: Inntektskilde = EN_ARBEIDSGIVER,
            warnings: List<Warning> = emptyList(),
            utbetalingstype: Utbetalingtype = UTBETALING,
            harVedtaksperiodePågåendeOverstyring: Boolean = false,
        ) = Utbetalingsfilter(
            fødselsnummer = fødselsnummer,
            delvisRefusjon = delvisRefusjon,
            erUtbetaltFør = false,
            harUtbetalingTilSykmeldt = harUtbetalingTilSykmeldt,
            periodetype = periodetype,
            inntektskilde = inntektskilde,
            warnings = warnings,
            utbetalingtype = utbetalingstype,
            harVedtaksperiodePågåendeOverstyring = harVedtaksperiodePågåendeOverstyring,
        )

        private fun assertKanUtbetales(filter: Utbetalingsfilter) {
            assertTrue(filter.kanUtbetales)
            assertFalse(filter.kanIkkeUtbetales)
            assertThrows<IllegalArgumentException> { filter.årsaker() }
        }

        private fun assertKanIkkeUtbetales(filter: Utbetalingsfilter, forventedeÅrsaker: List<String>) {
            assertFalse(filter.kanUtbetales)
            assertTrue(filter.kanIkkeUtbetales)
            assertEquals(forventedeÅrsaker, filter.årsaker())
        }
    }
}
