package no.nav.helse.modell.vilkårsprøving

import no.nav.helse.modell.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

internal class AvviksvurderingTest {
    @Test
    fun `avviksvurdering toDto`() {
        val unikId = UUID.randomUUID()
        val fødselsnummer = "12345678910"
        val skjæringstidspunkt = 1.januar
        val opprettet = LocalDateTime.now()
        val avviksprosent = 25.0
        val vilkårsgrunnlagId = UUID.randomUUID()

        val avviksvurdering =
            Avviksvurdering.gjenopprett(
                AvviksvurderingDto(
                    unikId = unikId,
                    vilkårsgrunnlagId = vilkårsgrunnlagId,
                    fødselsnummer = fødselsnummer,
                    skjæringstidspunkt = skjæringstidspunkt,
                    opprettet = opprettet,
                    avviksprosent = avviksprosent,
                    sammenligningsgrunnlag = sammenligningsgrunnlag(),
                    beregningsgrunnlag = beregningsggrunnlag(),
                ),
            )

        val dto = avviksvurdering.toDto()
        assertEquals(
            AvviksvurderingDto(
                unikId = unikId,
                vilkårsgrunnlagId = vilkårsgrunnlagId,
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                opprettet = opprettet,
                avviksprosent = avviksprosent,
                sammenligningsgrunnlag = sammenligningsgrunnlag(),
                beregningsgrunnlag = beregningsggrunnlag(),
            ),
            dto,
        )
    }

    @Test
    fun equals() {
        val unikId = UUID.randomUUID()
        val fødselsnummer = "12345678910"
        val skjæringstidspunkt = 1.januar
        val opprettet = LocalDateTime.now()
        val avviksprosent = 25.0

        val avviksvurdering =
            Avviksvurdering.gjenopprett(
                AvviksvurderingDto(
                    unikId = unikId,
                    vilkårsgrunnlagId = null,
                    fødselsnummer = fødselsnummer,
                    skjæringstidspunkt = skjæringstidspunkt,
                    opprettet = opprettet,
                    avviksprosent = avviksprosent,
                    sammenligningsgrunnlag = sammenligningsgrunnlag(),
                    beregningsgrunnlag = beregningsggrunnlag(),
                ),
            )

        assertEquals(avviksvurdering, avviksvurdering)
        assertEquals(avviksvurdering.hashCode(), avviksvurdering.hashCode())
        assertEquals(
            Avviksvurdering.gjenopprett(
                AvviksvurderingDto(
                    unikId = unikId,
                    vilkårsgrunnlagId = null,
                    fødselsnummer = fødselsnummer,
                    skjæringstidspunkt = skjæringstidspunkt,
                    opprettet = opprettet,
                    avviksprosent = avviksprosent,
                    sammenligningsgrunnlag = sammenligningsgrunnlag(),
                    beregningsgrunnlag = beregningsggrunnlag(),
                ),
            ),
            avviksvurdering,
        )
        assertEquals(
            Avviksvurdering.gjenopprett(
                AvviksvurderingDto(
                    unikId = unikId,
                    vilkårsgrunnlagId = null,
                    fødselsnummer = fødselsnummer,
                    skjæringstidspunkt = skjæringstidspunkt,
                    opprettet = opprettet,
                    avviksprosent = avviksprosent,
                    sammenligningsgrunnlag = sammenligningsgrunnlag(),
                    beregningsgrunnlag = beregningsggrunnlag(),
                ),
            ).hashCode(),
            avviksvurdering.hashCode(),
        )
    }

    private fun sammenligningsgrunnlag(): SammenligningsgrunnlagDto =
        SammenligningsgrunnlagDto(
            totalbeløp = 50000.0,
            innrapporterteInntekter = listOf(innrapportertInntekt()),
        )

    private fun innrapportertInntekt(): InnrapportertInntektDto =
        InnrapportertInntektDto(
            arbeidsgiverreferanse = "000000000",
            inntekter = listOf(inntekt()),
        )

    private fun inntekt(): InntektDto =
        InntektDto(
            årMåned = YearMonth.of(2018, 1),
            beløp = 50000.0,
        )

    private fun beregningsggrunnlag(): BeregningsgrunnlagDto =
        BeregningsgrunnlagDto(
            totalbeløp = 120000.0,
            omregnedeÅrsinntekter = listOf(omregnetÅrsinntekt()),
        )

    private fun omregnetÅrsinntekt(): OmregnetÅrsinntektDto =
        OmregnetÅrsinntektDto(
            arbeidsgiverreferanse = "000000000",
            beløp = 10000.0,
        )
}
