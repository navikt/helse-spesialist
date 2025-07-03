package no.nav.helse.spesialist.domain.tilkommeninntekt

import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.TilstandDto
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.testfixtures.des
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagSaksbehandlerident
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
        val fødselsnummer = lagFødselsnummer()
        val organisasjonsnummer = lagOrganisasjonsnummer()
        val tilkommenInntekt = TilkommenInntekt.ny(
            periode = (1 jan 2018) tilOgMed (31 jan 2018),
            ekskluderteUkedager = setOf(1 jan 2018, 31 jan 2018),
            periodebeløp = BigDecimal("10000.0"),
            fødselsnummer = fødselsnummer,
            saksbehandlerIdent = lagSaksbehandlerident(),
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
            vedtaksperioder = vedtaksperioder
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
            vedtaksperioder = vedtaksperioder
        )

        // Then:
        assertFalse(erInnenfor)
    }

    @Test
    fun `periode som slutter etter tom er ikke innenfor sykefraværstilfellet`() {
        // Given:
        val periode = 1 jan 2018 tilOgMed (1 feb 2018)
        val vedtaksperioder = listOf(lagVedtaksperiode(fom = 1 jan 2018, tom = 31 jan 2018))

        // When:
        val erInnenfor = TilkommenInntektPeriodeValidator.erInnenforEtSykefraværstilfelle(
            periode = periode,
            vedtaksperioder = vedtaksperioder
        )

        // Then:
        assertFalse(erInnenfor)
    }

    private fun lagVedtaksperiode(
        fom: LocalDate,
        tom: LocalDate
    ): VedtaksperiodeDto {
        val vedtaksperiodeId = UUID.randomUUID()
        return VedtaksperiodeDto(
            organisasjonsnummer = lagOrganisasjonsnummer(),
            vedtaksperiodeId = vedtaksperiodeId,
            forkastet = false,
            behandlinger = listOf(
                BehandlingDto(
                    id = UUID.randomUUID(),
                    vedtaksperiodeId = vedtaksperiodeId,
                    utbetalingId = UUID.randomUUID(),
                    spleisBehandlingId = UUID.randomUUID(),
                    skjæringstidspunkt = fom,
                    fom = fom,
                    tom = tom,
                    tilstand = TilstandDto.KlarTilBehandling,
                    tags = emptyList(),
                    vedtakBegrunnelse = null,
                    varsler = emptyList()
                )
            )
        )
    }
}
