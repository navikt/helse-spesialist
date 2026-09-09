package no.nav.helse.spesialist.domain.tilkommeninntekt

import no.nav.helse.spesialist.domain.Behandling.Companion.tilSykefraværstilfellePerioder
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.testfixtures.*
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.random.Random
import kotlin.test.Test

class TilkommenInntektPeriodeValidatorTest {
    @Test
    fun `kan ikke legge til periode som overlapper med annen periode`() {
        val identitetsnummer = lagIdentitetsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val tilkommenInntekt =
            TilkommenInntekt.ny(
                periode = (1 jan 2018) tilOgMed (31 jan 2018),
                ekskluderteUkedager = setOf(1 jan 2018, 31 jan 2018),
                periodebeløp = BigDecimal("10000.0"),
                identitetsnummer = identitetsnummer,
                saksbehandlerIdent = lagSaksbehandler().ident,
                notatTilBeslutter = "et notat til beslutter",
                totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                organisasjonsnummer = organisasjonsnummer,
            )

        assertThrows<IllegalStateException> {
            TilkommenInntektPeriodeValidator.validerAtNyPeriodeIkkeOverlapperEksisterendePerioder(
                periode = (15 jan 2018) tilOgMed (31 jan 2018),
                organisasjonsnummer = tilkommenInntekt.organisasjonsnummer,
                andreTilkomneInntekter = listOf(tilkommenInntekt),
            )
        }
    }

    @Test
    fun `periode som spenner hele fom og tom er innenfor sykefraværstilfellet`() {
        // Given:
        val periode = 1 jan 2018 tilOgMed (31 jan 2018)
        val behandlinger = listOf(lagBehandling(fom = 1 jan 2018, tom = 31 jan 2018))

        // When:
        val erInnenfor =
            TilkommenInntektPeriodeValidator.erInnenforEtSykefraværstilfelle(
                periode = periode,
                behandlinger = behandlinger,
            )

        // Then:
        assertTrue(erInnenfor)
    }

    @Test
    fun `periode som overlapper med flere perioder er innenfor sykefraværstilfellet`() {
        // Given:
        val periode = 1 jan 2018 tilOgMed (28 feb 2018)
        val behandlinger =
            listOf(
                lagBehandling(fom = 1 jan 2018, tom = 31 jan 2018),
                lagBehandling(fom = 1 feb 2018, tom = 28 feb 2018),
            )

        // When:
        val erInnenfor =
            TilkommenInntektPeriodeValidator.erInnenforEtSykefraværstilfelle(
                periode = periode,
                behandlinger = behandlinger,
            )

        // Then:
        assertTrue(erInnenfor)
    }

    @Test
    fun `periode som overlapper med usammenhengende perioder er ikke innenfor sykefraværstilfellet`() {
        // Given:
        val periode = 1 jan 2018 tilOgMed (28 feb 2018)
        val behandlinger =
            listOf(
                lagBehandling(fom = 1 jan 2018, tom = 20 jan 2018),
                lagBehandling(fom = 1 feb 2018, tom = 28 feb 2018),
            )

        // When:
        val erInnenfor =
            TilkommenInntektPeriodeValidator.erInnenforEtSykefraværstilfelle(
                periode = periode,
                behandlinger = behandlinger,
            )

        // Then:
        assertFalse(erInnenfor)
    }

    @Test
    fun `periode som dekker en tidligere periode når det fins nyere sykefraværstilfelle er innenfor`() {
        // Given:
        val periode = 1 jan 2018 tilOgMed (20 jan 2018)
        val behandlinger =
            listOf(
                lagBehandling(fom = 1 jan 2018, tom = 20 jan 2018),
                lagBehandling(fom = 1 feb 2018, tom = 28 feb 2018),
            )

        // When:
        val erInnenfor =
            TilkommenInntektPeriodeValidator.erInnenforEtSykefraværstilfelle(
                periode = periode,
                behandlinger = behandlinger,
            )

        // Then:
        assertTrue(erInnenfor)
    }

    @Test
    fun `periode som overlapper med over flere perioder for flere arbeidsgivere er innenfor`() {
        // Given:
        val periode = 15 jan 2018 tilOgMed (15 feb 2018)
        val behandlinger =
            listOf(
                lagBehandling(fom = 1 jan 2018, tom = 31 jan 2018),
                lagBehandling(fom = 1 feb 2018, tom = 28 feb 2018),
            )

        // When:
        val erInnenfor =
            TilkommenInntektPeriodeValidator.erInnenforEtSykefraværstilfelle(
                periode = periode,
                behandlinger = behandlinger,
            )

        // Then:
        assertTrue(erInnenfor)
    }

    @Test
    fun `periode som begynner før fom er ikke innenfor sykefraværstilfellet`() {
        // Given:
        val periode = 31 des 2017 tilOgMed (31 jan 2018)
        val behandlinger = listOf(lagBehandling(fom = 1 jan 2018, tom = 31 jan 2018))

        // When:
        val erInnenfor =
            TilkommenInntektPeriodeValidator.erInnenforEtSykefraværstilfelle(
                periode = periode,
                behandlinger = behandlinger,
            )

        // Then:
        assertFalse(erInnenfor)
    }

    @Test
    fun `usorterte perioder resulterer i en sammenhende periode`() {
        // Given:
        val periode = 12 jun 2025 tilOgMed (11 jul 2025)
        val behandlinger =
            listOf(
                lagBehandling(fom = 13 jun 2025, tom = 24 aug 2025),
                lagBehandling(fom = 25 mai 2025, tom = 12 jun 2025),
            )

        // When:
        val erInnenfor =
            TilkommenInntektPeriodeValidator.erInnenforEtSykefraværstilfelle(
                periode = periode,
                behandlinger = behandlinger,
            )

        // Then:
        assertTrue(erInnenfor)
    }

    @Test
    fun `periode som slutter etter tom er ikke innenfor sykefraværstilfellet`() {
        // Given:
        val periode = 1 jan 2018 tilOgMed (1 feb 2018)
        val behandlinger =
            listOf(
                lagBehandling(fom = 1 jan 2018, tom = 31 jan 2018),
                lagBehandling(fom = 1 jan 2018, tom = 31 jan 2018),
            )

        // When:
        val erInnenfor =
            TilkommenInntektPeriodeValidator.erInnenforEtSykefraværstilfelle(
                periode = periode,
                behandlinger = behandlinger,
            )

        // Then:
        assertFalse(erInnenfor)
    }

    @Test
    fun `slår sammen perioder som er inntil hverandre`() {
        val expected = Periode(1 jan 2018, 31 jan 2018)
        val behandling1 = lagBehandling(fom = expected.fom, tom = 10 jan 2018)
        val behandling2 = lagBehandling(fom = 11 jan 2018, tom = expected.tom)

        val sammenslåttePerioder = listOf(behandling1, behandling2).tilSykefraværstilfellePerioder()
        assertEquals(listOf(expected), sammenslåttePerioder)
    }

    @Test
    fun `slår ikke sammen perioder med minst en dag mellom dem`() {
        val behandling1 = lagBehandling(fom = 1 jan 2018, tom = 10 jan 2018)
        val behandling2 = lagBehandling(fom = 12 jan 2018, tom = 31 jan 2018)

        val sammenslåttePerioder = listOf(behandling1, behandling2).tilSykefraværstilfellePerioder()
        assertEquals(2, sammenslåttePerioder.size)
    }

    @Test
    fun `slår sammen perioder på tvers av arbeidsgivere`() {
        val behandling1 = lagBehandling(fom = 1 jan 2018, tom = 10 jan 2018)
        val behandling2 = lagBehandling(fom = 11 jan 2018, tom = 20 jan 2018)
        val behandling3 = lagBehandling(fom = 21 jan 2018, tom = 31 jan 2018)

        val sammenslåttePerioder = listOf(behandling1, behandling2, behandling3).tilSykefraværstilfellePerioder()
        assertEquals(1, sammenslåttePerioder.size)
    }

    @Test
    fun `slår sammen perioder om en annen arbeidsgiver knytter dem sammen`() {
        val behandling1 = lagBehandling(fom = 1 jan 2018, tom = 10 jan 2018)
        val behandling2 = lagBehandling(fom = 21 jan 2018, tom = 31 jan 2018)

        assertEquals(2, listOf(behandling1, behandling2).tilSykefraværstilfellePerioder().size)

        val behandling3 = lagBehandling(fom = 9 jan 2018, tom = 22 jan 2018)
        assertEquals(1, listOf(behandling1, behandling2, behandling3).tilSykefraværstilfellePerioder().size)
    }

    @Test
    fun `slår sammen perioder som overlapper litt hulter til bulter`() {
        val sykefraværstilfelle1 = Periode(1 jan 2018, 20 jan 2018)
        val sykefraværstilfelle2 = Periode(1 feb 2018, 20 feb 2018)
        val behandling1 = lagBehandling(fom = sykefraværstilfelle1.fom, tom = 10 jan 2018)
        val behandling2 = lagBehandling(fom = sykefraværstilfelle2.fom, tom = 10 feb 2018)
        val behandling3 = lagBehandling(fom = 11 jan 2018, tom = sykefraværstilfelle1.tom)
        val behandling4 = lagBehandling(fom = 11 feb 2018, tom = sykefraværstilfelle2.tom)

        assertEquals(
            listOf(sykefraværstilfelle1, sykefraværstilfelle2),
            listOf(behandling1, behandling2, behandling3, behandling4).tilSykefraværstilfellePerioder(),
        )
    }
}
