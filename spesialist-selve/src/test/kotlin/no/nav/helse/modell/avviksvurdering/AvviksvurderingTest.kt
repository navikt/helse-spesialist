package no.nav.helse.modell.avviksvurdering

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AvviksvurderingTest {

    @Test
    fun `avviksvurdering toDto`() {
        val unikId = UUID.randomUUID()
        val fødselsnummer = "12345678910"
        val skjæringstidspunkt = 1.januar
        val opprettet = LocalDateTime.now()
        val avviksprosent = 25.0

        val avviksvurdering = Avviksvurdering(
            unikId = unikId,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            opprettet = opprettet,
            avviksprosent = avviksprosent,
            sammenligningsgrunnlag = sammenligningsgrunnlag(unikId = unikId),
            beregningsgrunnlag = beregningsggrunnlag()
        )

        val dto = avviksvurdering.toDto()
        assertEquals(
            avviksvurderingDto(
                unikId = unikId,
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                opprettet = opprettet,
                avviksprosent = avviksprosent,
            ),
            dto
        )
    }

    @Test
    fun equals() {
        val unikId = UUID.randomUUID()
        val fødselsnummer = "12345678910"
        val skjæringstidspunkt = 1.januar
        val opprettet = LocalDateTime.now()
        val avviksprosent = 25.0

        val avviksvurdering = Avviksvurdering(
            unikId = unikId,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            opprettet = opprettet,
            avviksprosent = avviksprosent,
            sammenligningsgrunnlag = sammenligningsgrunnlag(unikId = unikId),
            beregningsgrunnlag = beregningsggrunnlag()
        )

        assertEquals(avviksvurdering, avviksvurdering)
        assertEquals(avviksvurdering.hashCode(), avviksvurdering.hashCode())
        assertEquals(
            Avviksvurdering(
                unikId = unikId,
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                opprettet = opprettet,
                avviksprosent = avviksprosent,
                sammenligningsgrunnlag = sammenligningsgrunnlag(unikId = unikId),
                beregningsgrunnlag = beregningsggrunnlag()
            ),
            avviksvurdering
        )
        assertEquals(
            Avviksvurdering(
                unikId = unikId,
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                opprettet = opprettet,
                avviksprosent = avviksprosent,
                sammenligningsgrunnlag = sammenligningsgrunnlag(unikId = unikId),
                beregningsgrunnlag = beregningsggrunnlag()
            ).hashCode(),
            avviksvurdering.hashCode()
        )
    }

    private fun avviksvurderingDto(
        fødselsnummer: String = "12345678910",
        skjæringstidspunkt: LocalDate = 1.januar,
        unikId: UUID = UUID.randomUUID(),
        opprettet: LocalDateTime = LocalDateTime.now(),
        avviksprosent: Double = 25.0,
    ): AvviksvurderingDto = AvviksvurderingDto(
        fødselsnummer = fødselsnummer,
        skjæringstidspunkt = skjæringstidspunkt,
        opprettet = opprettet,
        avviksprosent = avviksprosent,
        unikId = unikId,
        sammenligningsgrunnlag = sammenligningsgrunnlag(unikId),
        beregningsgrunnlag = beregningsggrunnlag()
    )

    private fun sammenligningsgrunnlag(unikId: UUID = UUID.randomUUID()): SammenligningsgrunnlagDto =
        SammenligningsgrunnlagDto(
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