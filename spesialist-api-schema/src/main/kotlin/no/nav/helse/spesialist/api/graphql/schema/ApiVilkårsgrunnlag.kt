package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year
import java.util.UUID

@GraphQLName("VilkarsgrunnlagV2")
sealed interface ApiVilkårsgrunnlagV2 {
    val id: UUID
    val skjaeringstidspunkt: LocalDate
    val sykepengegrunnlag: BigDecimal
}

@GraphQLName("VilkarsgrunnlagArbeidstaker")
sealed interface ApiVilkårsgrunnlagArbeidstaker : ApiVilkårsgrunnlagV2 {
    override val id: UUID
    override val skjaeringstidspunkt: LocalDate
    override val sykepengegrunnlag: BigDecimal
    val inntekter: List<ApiArbeidstakerinntektV2>
    val arbeidsgiverrefusjoner: List<ApiArbeidsgiverrefusjonV2>
}

@GraphQLName("VilkarsgrunnlagArbeidstakerInfotrygd")
data class ApiVilkårsgrunnlagArbeidstakerInfotrygd(
    override val id: UUID,
    override val skjaeringstidspunkt: LocalDate,
    override val sykepengegrunnlag: BigDecimal,
    override val inntekter: List<ApiArbeidstakerinntektInfotrygd>,
    override val arbeidsgiverrefusjoner: List<ApiArbeidsgiverrefusjonV2>,
    val omregnetArsinntekt: BigDecimal,
) : ApiVilkårsgrunnlagArbeidstaker

@GraphQLName("VilkarsgrunnlagArbeidstakerSpleis")
data class ApiVilkårsgrunnlagArbeidstakerSpleis(
    override val id: UUID,
    override val skjaeringstidspunkt: LocalDate,
    override val sykepengegrunnlag: BigDecimal,
    override val inntekter: List<ApiArbeidstakerinntektV2>,
    override val arbeidsgiverrefusjoner: List<ApiArbeidsgiverrefusjonV2>,
    val avviksvurdering: ApiVilkårsgrunnlagAvviksvurdering?,
    val kravOmMedlemskap: ApiVilkårsgrunnlagKrav,
    val kravOmMinstelonn: ApiVilkårsgrunnlagKravAlltidVurdert,
    val kravOmOpptjening: ApiVilkårsgrunnlagKravAlltidVurdert,
    val skjonnsmessigFastsattAarlig: BigDecimal?,
    val grunnbelop: Int,
    val sykepengegrunnlagsgrense: ApiSykepengegrunnlagsgrense,
    val antallOpptjeningsdagerErMinst: Int,
    val opptjeningFra: LocalDate,
) : ApiVilkårsgrunnlagArbeidstaker

@GraphQLName("VilkarsgrunnlagSelvstendig")
data class ApiVilkårsgrunnlagSelvstendig(
    override val id: UUID,
    override val skjaeringstidspunkt: LocalDate,
    override val sykepengegrunnlag: BigDecimal,
    val pensjonsgivendeInntekt: List<ApiPensjonsgivendeInntekt>,
    val grunnbelop: Int,
) : ApiVilkårsgrunnlagV2

@GraphQLName("VilkarsgrunnlagAvviksvurdering")
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

@GraphQLName("VilkarsgrunnlagKravAlltidVurdert")
enum class ApiVilkårsgrunnlagKravAlltidVurdert {
    OPPFYLT,
    IKKE_OPPFYLT,
}

@GraphQLName("VilkarsgrunnlagKrav")
enum class ApiVilkårsgrunnlagKrav {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_VURDERT,
}
