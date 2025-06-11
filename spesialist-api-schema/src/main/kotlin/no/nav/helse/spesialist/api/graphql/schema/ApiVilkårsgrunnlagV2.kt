package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@GraphQLName("VilkarsgrunnlagV2")
sealed interface ApiVilkårsgrunnlagV2 {
    val id: UUID
    val inntekter: List<ApiArbeidsgiverinntekt>
    val arbeidsgiverrefusjoner: List<ApiArbeidsgiverrefusjon>
    val skjaeringstidspunkt: LocalDate
    val sykepengegrunnlag: Double
}

@GraphQLName("VilkarsgrunnlagInfotrygdV2")
data class ApiVilkårsgrunnlagInfotrygdV2(
    override val id: UUID,
    override val inntekter: List<ApiArbeidsgiverinntekt>,
    override val arbeidsgiverrefusjoner: List<ApiArbeidsgiverrefusjon>,
    override val skjaeringstidspunkt: LocalDate,
    override val sykepengegrunnlag: Double,
    val omregnetArsinntekt: Double,
) : ApiVilkårsgrunnlagV2

@GraphQLName("VilkarsgrunnlagSpleisV2")
data class ApiVilkårsgrunnlagSpleisV2(
    override val id: UUID,
    override val inntekter: List<ApiArbeidsgiverinntekt>,
    override val skjaeringstidspunkt: LocalDate,
    override val sykepengegrunnlag: Double,
    override val arbeidsgiverrefusjoner: List<ApiArbeidsgiverrefusjon>,
    val avviksvurdering: ApiVilkårsgrunnlagAvviksvurdering?,
    val skjonnsmessigFastsattAarlig: Double?,
    val antallOpptjeningsdagerErMinst: Int,
    val grunnbelop: Int,
    val sykepengegrunnlagsgrense: ApiSykepengegrunnlagsgrense,
    val vurderingAvKravOmMedlemskap: ApiVilkårsgrunnlagVurdering,
    val oppfyllerKravOmMinstelonn: Boolean,
    val oppfyllerKravOmOpptjening: Boolean,
    val opptjeningFra: LocalDate,
    val beregningsgrunnlag: BigDecimal,
) : ApiVilkårsgrunnlagV2

@GraphQLName("VilkarsgrunnlagAvviksvurdering")
data class ApiVilkårsgrunnlagAvviksvurdering(
    val avviksprosent: BigDecimal,
    val beregningsgrunnlag: BigDecimal,
    val sammenligningsgrunnlag: BigDecimal,
)

@GraphQLName("VilkarsgrunnlagVurdering")
enum class ApiVilkårsgrunnlagVurdering {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_VURDERT,
}
