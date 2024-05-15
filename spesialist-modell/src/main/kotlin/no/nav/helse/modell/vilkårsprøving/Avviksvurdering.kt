package no.nav.helse.modell.vilkårsprøving

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class Avviksvurdering private constructor(
    private val unikId: UUID,
    private val vilkårsgrunnlagId: UUID?,
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val opprettet: LocalDateTime,
    private val avviksprosent: Double,
    private val sammenligningsgrunnlag: Sammenligningsgrunnlag,
    private val beregningsgrunnlag: Beregningsgrunnlag,
) {
    companion object {
        fun List<Avviksvurdering>.finnRiktigAvviksvurdering(skjæringstidspunkt: LocalDate) =
            filter { it.skjæringstidspunkt == skjæringstidspunkt }.maxByOrNull { it.opprettet }

        internal fun gjenopprett(avviksvurderingDto: AvviksvurderingDto): Avviksvurdering {
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

    override fun equals(other: Any?): Boolean =
        this === other || (
            other is Avviksvurdering &&
                javaClass == other.javaClass &&
                unikId == other.unikId &&
                vilkårsgrunnlagId == other.vilkårsgrunnlagId &&
                fødselsnummer == other.fødselsnummer &&
                skjæringstidspunkt == other.skjæringstidspunkt &&
                opprettet.withNano(0) == other.opprettet.withNano(0) &&
                avviksprosent == other.avviksprosent &&
                sammenligningsgrunnlag == other.sammenligningsgrunnlag &&
                beregningsgrunnlag == other.beregningsgrunnlag
        )

    override fun hashCode(): Int {
        var result = unikId.hashCode()
        result = 31 * result + vilkårsgrunnlagId.hashCode()
        result = 31 * result + fødselsnummer.hashCode()
        result = 31 * result + skjæringstidspunkt.hashCode()
        result = 31 * result + avviksprosent.hashCode()
        result = 31 * result + sammenligningsgrunnlag.hashCode()
        result = 31 * result + beregningsgrunnlag.hashCode()
        return result
    }
}

internal class Sammenligningsgrunnlag private constructor(
    private val totalbeløp: Double,
    private val innrapporterteInntekter: List<InnrapportertInntekt>,
) {
    internal fun toDto(): SammenligningsgrunnlagDto {
        return SammenligningsgrunnlagDto(
            totalbeløp = totalbeløp,
            innrapporterteInntekter = innrapporterteInntekter.map { it.toDto() },
        )
    }

    override fun equals(other: Any?): Boolean =
        this === other || (
            other is Sammenligningsgrunnlag &&
                totalbeløp == other.totalbeløp && innrapporterteInntekter == other.innrapporterteInntekter
        )

    override fun hashCode(): Int {
        var result = totalbeløp.hashCode()
        result = 31 * result + innrapporterteInntekter.hashCode()
        return result
    }

    internal companion object {
        internal fun gjenopprett(sammenligningsgrunnlag: SammenligningsgrunnlagDto) =
            Sammenligningsgrunnlag(
                totalbeløp = sammenligningsgrunnlag.totalbeløp,
                innrapporterteInntekter = sammenligningsgrunnlag.innrapporterteInntekter.map { InnrapportertInntekt.gjenopprett(it) },
            )
    }
}

internal class InnrapportertInntekt private constructor(
    private val arbeidsgiverreferanse: String,
    private val inntekter: List<Inntekt>,
) {
    internal fun toDto() =
        InnrapportertInntektDto(
            arbeidsgiverreferanse = arbeidsgiverreferanse,
            inntekter = inntekter.map { it.toDto() },
        )

    override fun equals(other: Any?): Boolean =
        this === other || (
            other is InnrapportertInntekt &&
                arbeidsgiverreferanse == other.arbeidsgiverreferanse &&
                inntekter == other.inntekter
        )

    override fun hashCode(): Int {
        var result = arbeidsgiverreferanse.hashCode()
        result = 31 * result + inntekter.hashCode()
        return result
    }

    internal companion object {
        internal fun gjenopprett(innrapportertInntektDto: InnrapportertInntektDto) =
            InnrapportertInntekt(
                arbeidsgiverreferanse = innrapportertInntektDto.arbeidsgiverreferanse,
                inntekter = innrapportertInntektDto.inntekter.map { Inntekt.gjenopprett(it) },
            )
    }
}

internal class Inntekt private constructor(
    private val årMåned: YearMonth,
    private val beløp: Double,
) {
    internal fun toDto() = InntektDto(årMåned = årMåned, beløp = beløp)

    override fun equals(other: Any?) =
        this === other || (
            other is Inntekt && årMåned == other.årMåned && beløp == other.beløp
        )

    override fun hashCode(): Int {
        var result = årMåned.hashCode()
        result = 31 * result + beløp.hashCode()
        return result
    }

    internal companion object {
        internal fun gjenopprett(inntektDto: InntektDto) =
            Inntekt(
                årMåned = inntektDto.årMåned,
                beløp = inntektDto.beløp,
            )
    }
}

internal class Beregningsgrunnlag private constructor(
    private val totalbeløp: Double,
    private val omregnedeÅrsinntekter: List<OmregnetÅrsinntekt>,
) {
    internal fun toDto() =
        BeregningsgrunnlagDto(
            totalbeløp = totalbeløp,
            omregnedeÅrsinntekter = omregnedeÅrsinntekter.map { it.toDto() },
        )

    override fun equals(other: Any?) =
        this === other || (
            other is Beregningsgrunnlag &&
                totalbeløp == other.totalbeløp &&
                omregnedeÅrsinntekter == other.omregnedeÅrsinntekter
        )

    override fun hashCode(): Int {
        var result = totalbeløp.hashCode()
        result = 31 * result + omregnedeÅrsinntekter.hashCode()
        return result
    }

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

internal class OmregnetÅrsinntekt private constructor(
    private val arbeidsgiverreferanse: String,
    private val beløp: Double,
) {
    internal fun toDto() = OmregnetÅrsinntektDto(arbeidsgiverreferanse = arbeidsgiverreferanse, beløp = beløp)

    override fun equals(other: Any?) =
        this === other || (
            other is OmregnetÅrsinntekt &&
                other.arbeidsgiverreferanse == arbeidsgiverreferanse &&
                beløp == other.beløp
        )

    override fun hashCode(): Int {
        var result = arbeidsgiverreferanse.hashCode()
        result = 31 * result + beløp.hashCode()
        return result
    }

    internal companion object {
        internal fun gjenopprett(omregnetÅrsinntektDto: OmregnetÅrsinntektDto) =
            OmregnetÅrsinntekt(
                arbeidsgiverreferanse = omregnetÅrsinntektDto.arbeidsgiverreferanse,
                beløp = omregnetÅrsinntektDto.beløp,
            )
    }
}
