package no.nav.helse.spesialist.domain.tilkommeninntekt

import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.TilstandDto
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingId
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.testfixtures.aug
import no.nav.helse.spesialist.domain.testfixtures.des
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.jul
import no.nav.helse.spesialist.domain.testfixtures.jun
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.mai
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntektPeriodeValidator.tilSykefraværstillfellePerioder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random
import kotlin.test.Test

class TilkommenInntektPeriodeValidatorTest {
    @Test
    fun `kan ikke legge til periode som overlapper med annen periode`() {
        val identitetsnummer = lagIdentitetsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val tilkommenInntekt = TilkommenInntekt.ny(
            periode = (1 jan 2018) tilOgMed (31 jan 2018),
            ekskluderteUkedager = setOf(1 jan 2018, 31 jan 2018),
            periodebeløp = BigDecimal("10000.0"),
            identitetsnummer = identitetsnummer,
            saksbehandlerIdent = lagSaksbehandler().ident,
            notatTilBeslutter = "et notat til beslutter",
            totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
            organisasjonsnummer = organisasjonsnummer
        )

        assertThrows<IllegalStateException> {
            TilkommenInntektPeriodeValidator.validerAtNyPeriodeIkkeOverlapperEksisterendePerioder(
                periode = (15 jan 2018) tilOgMed (31 jan 2018),
                organisasjonsnummer = organisasjonsnummer,
                andreTilkomneInntekter = listOf(tilkommenInntekt)
            )
        }
    }

    @Test
    fun `periode som spenner hele fom og tom er innenfor sykefraværstilfellet`() {
        // Given:
        val periode = 1 jan 2018 tilOgMed (31 jan 2018)
        val vedtaksperioder = listOf(lagVedtaksperiode(fom = 1 jan 2018, tom = 31 jan 2018))

        // When:
        val erInnenfor = TilkommenInntektPeriodeValidator.erInnenforEtSykefraværstilfelle(
            periode = periode,
            vedtaksperioder = vedtaksperioder,
        )

        // Then:
        assertTrue(erInnenfor)
    }

    @Test
    fun `periode som overlapper med flere perioder er innenfor sykefraværstilfellet`() {
        // Given:
        val periode = 1 jan 2018 tilOgMed (28 feb 2018)
        val vedtaksperioder = listOf(
            lagVedtaksperiode(fom = 1 jan 2018, tom = 31 jan 2018),
            lagVedtaksperiode(fom = 1 feb 2018, tom = 28 feb 2018)
        )

        // When:
        val erInnenfor = TilkommenInntektPeriodeValidator.erInnenforEtSykefraværstilfelle(
            periode = periode,
            vedtaksperioder = vedtaksperioder,
        )

        // Then:
        assertTrue(erInnenfor)
    }

    @Test
    fun `periode som overlapper med usammenhengende perioder er ikke innenfor sykefraværstilfellet`() {
        // Given:
        val periode = 1 jan 2018 tilOgMed (28 feb 2018)
        val vedtaksperioder = listOf(
            lagVedtaksperiode(fom = 1 jan 2018, tom = 20 jan 2018),
            lagVedtaksperiode(fom = 1 feb 2018, tom = 28 feb 2018)
        )

        // When:
        val erInnenfor = TilkommenInntektPeriodeValidator.erInnenforEtSykefraværstilfelle(
            periode = periode,
            vedtaksperioder = vedtaksperioder,
        )

        // Then:
        assertFalse(erInnenfor)
    }

    @Test
    fun `periode som dekker en tidligere periode når det fins nyere sykefraværstilfelle er innenfor`() {
        // Given:
        val periode = 1 jan 2018 tilOgMed (20 jan 2018)
        val vedtaksperioder = listOf(
            lagVedtaksperiode(fom = 1 jan 2018, tom = 20 jan 2018),
            lagVedtaksperiode(fom = 1 feb 2018, tom = 28 feb 2018)
        )

        // When:
        val erInnenfor = TilkommenInntektPeriodeValidator.erInnenforEtSykefraværstilfelle(
            periode = periode,
            vedtaksperioder = vedtaksperioder,
        )

        // Then:
        assertTrue(erInnenfor)
    }

    @Test
    fun `periode som overlapper med over flere perioder for flere arbeidsgivere er innenfor`() {
        // Given:
        val periode = 15 jan 2018 tilOgMed (15 feb 2018)
        val ag1 = lagOrganisasjonsnummer()
        val ag2 = lagOrganisasjonsnummer()
        val vedtaksperioder = listOf(
            lagVedtaksperiode(fom = 1 jan 2018, tom = 31 jan 2018, organisasjonsnummer = ag1),
            lagVedtaksperiode(fom = 1 feb 2018, 28 feb 2018, organisasjonsnummer = ag2),
        )

        // When:
        val erInnenfor = TilkommenInntektPeriodeValidator.erInnenforEtSykefraværstilfelle(
            periode = periode,
            vedtaksperioder = vedtaksperioder,
        )

        // Then:
        assertTrue(erInnenfor)
    }

    @Test
    fun `periode som begynner før fom er ikke innenfor sykefraværstilfellet`() {
        // Given:
        val periode = 31 des 2017 tilOgMed (31 jan 2018)
        val vedtaksperioder = listOf(lagVedtaksperiode(fom = 1 jan 2018, tom = 31 jan 2018))

        // When:
        val erInnenfor = TilkommenInntektPeriodeValidator.erInnenforEtSykefraværstilfelle(
            periode = periode,
            vedtaksperioder = vedtaksperioder,
        )

        // Then:
        assertFalse(erInnenfor)
    }

    @Test
    fun `usorterte perioder resulterer i en sammenhende periode`() {
        // Given:
        val periode = 12 jun 2025 tilOgMed (11 jul 2025)
        val vedtaksperioder = listOf(
            lagVedtaksperiode(fom = 13 jun 2025, tom = 24 aug 2025),
            lagVedtaksperiode(fom = 25 mai 2025, tom = 12 jun 2025),
        )

        // When:
        val erInnenfor = TilkommenInntektPeriodeValidator.erInnenforEtSykefraværstilfelle(
            periode = periode,
            vedtaksperioder = vedtaksperioder,
        )

        // Then:
        assertTrue(erInnenfor)
    }

    @Test
    fun `periode som slutter etter tom er ikke innenfor sykefraværstilfellet`() {
        // Given:
        val periode = 1 jan 2018 tilOgMed (1 feb 2018)
        val vedtaksperioder = listOf(
            lagVedtaksperiode(fom = 1 jan 2018, tom = 31 jan 2018),
            lagVedtaksperiode(fom = 1 jan 2018, tom = 31 jan 2018)
        )

        // When:
        val erInnenfor = TilkommenInntektPeriodeValidator.erInnenforEtSykefraværstilfelle(
            periode = periode,
            vedtaksperioder = vedtaksperioder,
        )

        // Then:
        assertFalse(erInnenfor)
    }

    @Test
    fun `slår sammen perioder som er inntil hverandre`() {
        val expected = Periode(1 jan 2018, 31 jan 2018)
        val periode1 = lagVedtaksperiode(fom = expected.fom, tom = 10 jan 2018)
        val periode2 = lagVedtaksperiode(fom = 11 jan 2018, tom = expected.tom)

        val sammenslåttePerioder = listOf(periode1, periode2).tilSykefraværstillfellePerioder()
        assertEquals(listOf(expected), sammenslåttePerioder)
    }

    @Test
    fun `slår ikke sammen perioder med minst en dag mellom dem`() {
        val periode1 = lagVedtaksperiode(fom = 1 jan 2018, tom = 10 jan 2018)
        val periode2 = lagVedtaksperiode(fom = 12 jan 2018, tom = 31 jan 2018)

        val sammenslåttePerioder = listOf(periode1, periode2).tilSykefraværstillfellePerioder()
        assertEquals(2, sammenslåttePerioder.size)
    }

    @Test
    fun `slår sammen perioder på tvers av arbeidsgivere`() {
        val ag1 = lagOrganisasjonsnummer()
        val ag2 = lagOrganisasjonsnummer()
        val periode1 = lagVedtaksperiode(fom = 1 jan 2018, tom = 10 jan 2018, organisasjonsnummer = ag1)
        val periode2 = lagVedtaksperiode(fom = 11 jan 2018, tom = 20 jan 2018, organisasjonsnummer = ag2)
        val periode3 = lagVedtaksperiode(fom = 21 jan 2018, tom = 31 jan 2018, organisasjonsnummer = ag1)

        val sammenslåttePerioder = listOf(periode1, periode2, periode3).tilSykefraværstillfellePerioder()
        assertEquals(1, sammenslåttePerioder.size)
    }

    @Test
    fun `slår sammen perioder om en annen arbeidsgiver knytter dem sammen`() {
        val ag1 = lagOrganisasjonsnummer()
        val ag2 = lagOrganisasjonsnummer()
        val periode1 = lagVedtaksperiode(fom = 1 jan 2018, tom = 10 jan 2018, organisasjonsnummer = ag1)
        val periode2 = lagVedtaksperiode(fom = 21 jan 2018, tom = 31 jan 2018, organisasjonsnummer = ag1)

        assertEquals(2, listOf(periode1, periode2).tilSykefraværstillfellePerioder().size)

        val periode3 = lagVedtaksperiode(fom = 9 jan 2018, tom = 22 jan 2018, organisasjonsnummer = ag2)
        assertEquals(1, listOf(periode1, periode2, periode3).tilSykefraværstillfellePerioder().size)
    }

    @Test
    fun `slår sammen perioder som overlapper litt hulter til bulter`() {
        val sykefraværstilfelle1 = Periode(1 jan 2018, 20 jan 2018)
        val sykefraværstilfelle2 = Periode(1 feb 2018, 20 feb 2018)
        val ag1 = lagOrganisasjonsnummer()
        val ag2 = lagOrganisasjonsnummer()
        val periode1 = lagVedtaksperiode(fom = sykefraværstilfelle1.fom, tom = 10 jan 2018, organisasjonsnummer = ag1)
        val periode2 = lagVedtaksperiode(fom = sykefraværstilfelle2.fom, tom = 10 feb 2018, organisasjonsnummer = ag1)
        val periode3 = lagVedtaksperiode(fom = 11 jan 2018, tom = sykefraværstilfelle1.tom, organisasjonsnummer = ag2)
        val periode4 = lagVedtaksperiode(fom = 11 feb 2018, tom = sykefraværstilfelle2.tom, organisasjonsnummer = ag2)

        assertEquals(
            listOf(sykefraværstilfelle1, sykefraværstilfelle2),
            listOf(periode1, periode2, periode3, periode4).tilSykefraværstillfellePerioder()
        )
    }

    private fun lagVedtaksperiode(
        fom: LocalDate,
        tom: LocalDate,
        skjæringstidspunkt: LocalDate = fom,
        organisasjonsnummer: String = lagOrganisasjonsnummer()
    ): VedtaksperiodeDto {
        val vedtaksperiodeId = UUID.randomUUID()
        return VedtaksperiodeDto(
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            forkastet = false,
            behandlinger = listOf(
                BehandlingDto(
                    id = UUID.randomUUID(),
                    vedtaksperiodeId = vedtaksperiodeId,
                    utbetalingId = UUID.randomUUID(),
                    spleisBehandlingId = UUID.randomUUID(),
                    skjæringstidspunkt = skjæringstidspunkt,
                    fom = fom,
                    tom = tom,
                    tilstand = TilstandDto.KlarTilBehandling,
                    tags = emptyList(),
                    vedtakBegrunnelse = null,
                    varsler = emptyList(),
                    yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER
                )
            )
        )
    }
}
