package no.nav.helse.modell.utbetaling

import no.nav.helse.modell.utbetaling.Utbetalingtype.REVURDERING
import no.nav.helse.modell.utbetaling.Utbetalingtype.UTBETALING
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
                erUtbetaltFør = false,
                harUtbetalingTilSykmeldt = false,
                periodetype = OVERGANG_FRA_IT,
                inntektskilde = FLERE_ARBEIDSGIVERE,
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
                erUtbetaltFør = false,
                harUtbetalingTilSykmeldt = false,
                periodetype = OVERGANG_FRA_IT,
                inntektskilde = FLERE_ARBEIDSGIVERE,
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
                erUtbetaltFør = true,
                harUtbetalingTilSykmeldt = false,
                periodetype = OVERGANG_FRA_IT,
                inntektskilde = FLERE_ARBEIDSGIVERE,
                utbetalingtype = UTBETALING,
                harVedtaksperiodePågåendeOverstyring = false,
            )
        )
    }

    @Test
    fun `endring, endring fra full refusjon til delvis, kan utbetales`() {
        fun utbetalingsfilter() = Utbetalingsfilter(
            fødselsnummer = "21111111111",
            erUtbetaltFør = true,
            harUtbetalingTilSykmeldt = true,
            periodetype = FORLENGELSE,
            inntektskilde = EN_ARBEIDSGIVER,
            utbetalingtype = UTBETALING,
            harVedtaksperiodePågåendeOverstyring = false,
        )
        assertKanUtbetales(utbetalingsfilter())
    }

    @Test
    fun `delvis refusjon kan utbetales dersom vedtaksperioden har pågående overstyring`() {
        assertKanUtbetales(
            utbetalingsfilter(harVedtaksperiodePågåendeOverstyring = true)
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
    fun `fødselsdato på dag 1 til 14 i måneden kan ikke utbetales`() {
        (1 .. 28).forEach { dag ->
            val fnr = ("0" + dag + "1".repeat(9)).takeLast(11)
            assertKanIkkeUtbetales(
                utbetalingsfilter(fødselsnummer = fnr),
                listOf("Brukerutbetalingsfilter: Velges ikke ut som 'to om dagen'")
            )
        }
    }

    @Test
    fun `d-nummer kan ikke utbetales`() {
        (41 .. 71).forEach { dag ->
            val fnr = dag.toString() + "1".repeat(9)
            assertKanIkkeUtbetales(
                utbetalingsfilter(fødselsnummer = fnr),
                listOf("Brukerutbetalingsfilter: Velges ikke ut som 'to om dagen'")
            )
        }
    }

    @Test
    fun `fødselsdato på dag 15 til 31 i måneden kan utbetales`() {
        (29 .. 31).forEach { dag ->
            val fnr = dag.toString() + "1".repeat(9)
            assertKanUtbetales(
                utbetalingsfilter(fødselsnummer = fnr)
            )
        }
    }

    @Test
    fun `ingen refusjon & inntektskilde fra flere arbeidsgivere kan ikke utbetales`() {
        assertKanIkkeUtbetales(
            utbetalingsfilter(inntektskilde = FLERE_ARBEIDSGIVERE),
            listOf("Brukerutbetalingsfilter: Inntektskilden er ikke for en arbeidsgiver")
        )
    }

    @Test
    fun `revurdering kan utbetales tross warnings`() {
        assertKanUtbetales(utbetalingsfilter(utbetalingstype = REVURDERING))
    }

    @Test
    fun `revurdering beholdes uavhengig av fødselsdato`() {
        assertKanUtbetales(
            utbetalingsfilter(
                fødselsnummer = "10111111111",
                utbetalingstype = REVURDERING
            )
        )
    }

    @Test
    fun `passerer ingen av kriteriene i filteret`() {
        assertKanIkkeUtbetales(
            Utbetalingsfilter(
                fødselsnummer = "14111111111",
                erUtbetaltFør = false,
                harUtbetalingTilSykmeldt = true,
                periodetype = OVERGANG_FRA_IT,
                inntektskilde = FLERE_ARBEIDSGIVERE,
                utbetalingtype = UTBETALING,
                harVedtaksperiodePågåendeOverstyring = false,
            ), listOf(
                "Brukerutbetalingsfilter: Velges ikke ut som 'to om dagen'",
                "Brukerutbetalingsfilter: Perioden er ikke førstegangsbehandling eller forlengelse",
                "Brukerutbetalingsfilter: Inntektskilden er ikke for en arbeidsgiver",
            )
        )
    }

    private companion object {
        private fun utbetalingsfilter(
            fødselsnummer: String = "31111111111",
            harUtbetalingTilSykmeldt: Boolean = true,
            periodetype: Periodetype = FØRSTEGANGSBEHANDLING,
            inntektskilde: Inntektskilde = EN_ARBEIDSGIVER,
            utbetalingstype: Utbetalingtype = UTBETALING,
            harVedtaksperiodePågåendeOverstyring: Boolean = false,
        ) = Utbetalingsfilter(
            fødselsnummer = fødselsnummer,
            erUtbetaltFør = false,
            harUtbetalingTilSykmeldt = harUtbetalingTilSykmeldt,
            periodetype = periodetype,
            inntektskilde = inntektskilde,
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
