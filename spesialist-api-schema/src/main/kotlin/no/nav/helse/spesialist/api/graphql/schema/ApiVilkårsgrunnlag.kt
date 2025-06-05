package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year
import java.util.UUID

@GraphQLName("VilkårsgrunnlagV2")
sealed interface ApiVilkårsgrunnlagV2 {
    val id: UUID
    val skjaeringstidspunkt: LocalDate
    val sykepengegrunnlag: BigDecimal
}

@GraphQLName("VilkårsgrunnlagArbeidstaker")
sealed interface ApiVilkårsgrunnlagArbeidstaker : ApiVilkårsgrunnlagV2 {
    override val id: UUID
    override val skjaeringstidspunkt: LocalDate
    override val sykepengegrunnlag: BigDecimal
    val inntekter: List<ApiArbeidsgiverinntektV2>
    val arbeidsgiverrefusjoner: List<ApiArbeidsgiverrefusjonV2>
}

@GraphQLName("VilkårsgrunnlagArbeidstakerInfotrygd")
data class ApiVilkårsgrunnlagArbeidstakerInfotrygd(
    override val id: UUID,
    override val skjaeringstidspunkt: LocalDate,
    override val sykepengegrunnlag: BigDecimal,
    override val inntekter: List<ApiArbeidsgiverinntektInfotrygd>,
    override val arbeidsgiverrefusjoner: List<ApiArbeidsgiverrefusjonV2>,
    val omregnetArsinntekt: BigDecimal,
) : ApiVilkårsgrunnlagArbeidstaker

@GraphQLName("VilkårsgrunnlagArbeidstakerSpleis")
data class ApiVilkårsgrunnlagArbeidstakerSpleis(
    override val id: UUID,
    override val skjaeringstidspunkt: LocalDate,
    override val sykepengegrunnlag: BigDecimal,
    override val inntekter: List<ApiArbeidsgiverinntektSpleis>,
    override val arbeidsgiverrefusjoner: List<ApiArbeidsgiverrefusjonV2>,
    val avviksvurdering: ApiVilkårsgrunnlagAvviksvurdering?,
    val oppfylteKrav: Set<ApiVilkårsgrunnlagKrav>,
    val skjonnsmessigFastsattAarlig: BigDecimal?,
    val grunnbelop: Int,
    val sykepengegrunnlagsgrense: ApiSykepengegrunnlagsgrense,
    val antallOpptjeningsdagerErMinst: Int,
    val opptjeningFra: LocalDate,
) : ApiVilkårsgrunnlagArbeidstaker

@GraphQLName("VilkårsgrunnlagSelvstendig")
data class ApiVilkårsgrunnlagSelvstendig(
    override val id: UUID,
    override val skjaeringstidspunkt: LocalDate,
    override val sykepengegrunnlag: BigDecimal,
    val pensjonsgivendeInntekt: List<ApiPensjonsgivendeInntekt>,
    val grunnbelop: Int,
) : ApiVilkårsgrunnlagV2

@GraphQLName("VilkårsgrunnlagAvviksvurdering")
data class ApiVilkårsgrunnlagAvviksvurdering(
    val avviksprosent: BigDecimal,
    val omregnetArsinntekt: BigDecimal,
    val sammenligningsgrunnlag: BigDecimal,
)

@GraphQLName("PensjonsgivendeInntekt")
data class ApiPensjonsgivendeInntekt(
    val ar: Year,
    val sum: BigDecimal,
)

@GraphQLName("VilkårsgrunnlagKrav")
enum class ApiVilkårsgrunnlagKrav {
    MINSTELONN,
    MEDLEMSKAP,
    OPPTJENING,
}
