package no.nav.helse.db

import DatabaseIntegrationTest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.modell.avviksvurdering.Avviksvurdering
import no.nav.helse.modell.avviksvurdering.AvviksvurderingDto
import no.nav.helse.modell.avviksvurdering.BeregningsgrunnlagDto
import no.nav.helse.modell.avviksvurdering.InnrapportertInntektDto
import no.nav.helse.modell.avviksvurdering.InntektDto
import no.nav.helse.modell.avviksvurdering.OmregnetÅrsinntektDto
import no.nav.helse.modell.avviksvurdering.SammenligningsgrunnlagDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class AvviksvurderingDaoTest: DatabaseIntegrationTest() {

    private val avviksvurderingDao = AvviksvurderingDao(dataSource)

    @Test
    fun `lagre avviksvurdering`() {
        val skjæringstidspunkt = 1.januar
        avviksvurderingDao.lagre(avviksvurdering(FNR, skjæringstidspunkt))
        assertNotNull(avviksvurderingDao.finnAvviksvurdering(FNR, skjæringstidspunkt))
    }

    @Test
    fun `finner avviksvurderinge basert på fødselsnummer og skjæringstidspunkt`() {
        val skjæringstidspunkt = 1.januar
        val unikId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        avviksvurderingDao.lagre(avviksvurdering(FNR, skjæringstidspunkt, unikId, opprettet))

        val funnetAvviksvurdering = avviksvurderingDao.finnAvviksvurdering(FNR, skjæringstidspunkt)
        val forventetAvviksvurdering = forventetAvviksvurdering(FNR, skjæringstidspunkt, unikId, opprettet)
        assertEquals(forventetAvviksvurdering, funnetAvviksvurdering)
    }

    private fun forventetAvviksvurdering(fødselsnummer: String, skjæringstidspunkt: LocalDate, unikId: UUID, opprettet: LocalDateTime): Avviksvurdering {
        return Avviksvurdering(
            unikId = unikId,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            opprettet = opprettet,
            avviksprosent = 26.0,
            sammenligningsgrunnlag = sammenligningsgrunnlag(unikId),
            beregningsgrunnlag = beregningsggrunnlag()
        )
    }

    private fun avviksvurdering(fødselsnummer: String = "12345678910", skjæringstidspunkt: LocalDate = 1.januar, unikId: UUID = UUID.randomUUID(), opprettet: LocalDateTime = LocalDateTime.now()): AvviksvurderingDto = AvviksvurderingDto(
        fødselsnummer = fødselsnummer,
        skjæringstidspunkt = skjæringstidspunkt,
        opprettet = opprettet,
        avviksprosent = 26.0,
        unikId = unikId,
        sammenligningsgrunnlag = sammenligningsgrunnlag(unikId),
        beregningsgrunnlag = beregningsggrunnlag()
    )

    private fun sammenligningsgrunnlag(unikId: UUID = UUID.randomUUID()): SammenligningsgrunnlagDto = SammenligningsgrunnlagDto(
        unikId = unikId,
        totalbeløp = 50000.0,
        innrapporterteInntekter = listOf(innrapportertInntekt())
    )

    private fun innrapportertInntekt(): InnrapportertInntektDto = InnrapportertInntektDto(
        arbeidsgiverreferanse = "000000000",
        inntekter = listOf(inntekt())
    )

    private fun inntekt(): InntektDto = InntektDto(
        årMåned = YearMonth.of(2018, 1),
        beløp = 50000.0
    )

    private fun beregningsggrunnlag(): BeregningsgrunnlagDto = BeregningsgrunnlagDto(
        totalbeløp = 120000.0,
        omregnedeÅrsinntekter = listOf(omregnetÅrsinntekt())
    )

    private fun omregnetÅrsinntekt(): OmregnetÅrsinntektDto = OmregnetÅrsinntektDto(
        arbeidsgiverreferanse = "000000000",
        beløp = 10000.0
    )
}