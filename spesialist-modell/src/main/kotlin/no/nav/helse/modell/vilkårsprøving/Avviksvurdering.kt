package no.nav.helse.modell.vilkårsprøving

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

data class Avviksvurdering(
    val unikId: UUID,
    val vilkårsgrunnlagId: UUID?,
    val fødselsnummer: String,
    val skjæringstidspunkt: LocalDate,
    val opprettet: LocalDateTime,
    val avviksprosent: Double,
    val sammenligningsgrunnlag: Sammenligningsgrunnlag,
    val beregningsgrunnlag: Beregningsgrunnlag,
) {
    companion object {
        fun List<Avviksvurdering>.finnRiktigAvviksvurdering(skjæringstidspunkt: LocalDate) =
            filter { it.skjæringstidspunkt == skjæringstidspunkt }.maxByOrNull { it.opprettet }

        fun gjenopprett(avviksvurderingDto: AvviksvurderingDto): Avviksvurdering {
            return Avviksvurdering(
                unikId = avviksvurderingDto.unikId,
                vilkårsgrunnlagId = avviksvurderingDto.vilkårsgrunnlagId,
                fødselsnummer = avviksvurderingDto.fødselsnummer,
                skjæringstidspunkt = avviksvurderingDto.skjæringstidspunkt,
                opprettet = avviksvurderingDto.opprettet,
                avviksprosent = avviksvurderingDto.avviksprosent,
                sammenligningsgrunnlag = Sammenligningsgrunnlag.gjenopprett(avviksvurderingDto.sammenligningsgrunnlag),
                beregningsgrunnlag = Beregningsgrunnlag.gjenopprett(avviksvurderingDto.beregningsgrunnlag),
            )
        }

        fun List<AvviksvurderingDto>.gjenopprett() = map { dto -> gjenopprett(dto) }
    }

    fun toDto() =
        AvviksvurderingDto(
            unikId = unikId,
            vilkårsgrunnlagId = vilkårsgrunnlagId,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            opprettet = opprettet,
            avviksprosent = avviksprosent,
            sammenligningsgrunnlag = sammenligningsgrunnlag.toDto(),
            beregningsgrunnlag = beregningsgrunnlag.toDto(),
        )
}

data class Sammenligningsgrunnlag(
    val totalbeløp: Double,
    val innrapporterteInntekter: List<InnrapportertInntekt>,
) {
    internal fun toDto(): SammenligningsgrunnlagDto {
        return SammenligningsgrunnlagDto(
            totalbeløp = totalbeløp,
            innrapporterteInntekter = innrapporterteInntekter.map { it.toDto() },
        )
    }

    internal companion object {
        internal fun gjenopprett(sammenligningsgrunnlag: SammenligningsgrunnlagDto) =
            Sammenligningsgrunnlag(
                totalbeløp = sammenligningsgrunnlag.totalbeløp,
                innrapporterteInntekter = sammenligningsgrunnlag.innrapporterteInntekter.map { InnrapportertInntekt.gjenopprett(it) },
            )
    }
}

data class InnrapportertInntekt(
    val arbeidsgiverreferanse: String,
    val inntekter: List<Inntekt>,
) {
    internal fun toDto() =
        InnrapportertInntektDto(
            arbeidsgiverreferanse = arbeidsgiverreferanse,
            inntekter = inntekter.map { it.toDto() },
        )

    internal companion object {
        internal fun gjenopprett(innrapportertInntektDto: InnrapportertInntektDto) =
            InnrapportertInntekt(
                arbeidsgiverreferanse = innrapportertInntektDto.arbeidsgiverreferanse,
                inntekter = innrapportertInntektDto.inntekter.map { Inntekt.gjenopprett(it) },
            )
    }
}

data class Inntekt(
    val årMåned: YearMonth,
    val beløp: Double,
) {
    internal fun toDto() = InntektDto(årMåned = årMåned, beløp = beløp)

    internal companion object {
        internal fun gjenopprett(inntektDto: InntektDto) =
            Inntekt(
                årMåned = inntektDto.årMåned,
                beløp = inntektDto.beløp,
            )
    }
}

data class Beregningsgrunnlag(
    val totalbeløp: Double,
    val omregnedeÅrsinntekter: List<OmregnetÅrsinntekt>,
) {
    internal fun toDto() =
        BeregningsgrunnlagDto(
            totalbeløp = totalbeløp,
            omregnedeÅrsinntekter = omregnedeÅrsinntekter.map { it.toDto() },
        )

    internal companion object {
        internal fun gjenopprett(beregningsgrunnlagDto: BeregningsgrunnlagDto) =
            Beregningsgrunnlag(
                totalbeløp = beregningsgrunnlagDto.totalbeløp,
                omregnedeÅrsinntekter =
                    beregningsgrunnlagDto.omregnedeÅrsinntekter.map {
                        OmregnetÅrsinntekt.gjenopprett(it)
                    },
            )
    }
}

data class OmregnetÅrsinntekt(
    val arbeidsgiverreferanse: String,
    val beløp: Double,
) {
    internal fun toDto() = OmregnetÅrsinntektDto(arbeidsgiverreferanse = arbeidsgiverreferanse, beløp = beløp)

    internal companion object {
        internal fun gjenopprett(omregnetÅrsinntektDto: OmregnetÅrsinntektDto) =
            OmregnetÅrsinntekt(
                arbeidsgiverreferanse = omregnetÅrsinntektDto.arbeidsgiverreferanse,
                beløp = omregnetÅrsinntektDto.beløp,
            )
    }
}
