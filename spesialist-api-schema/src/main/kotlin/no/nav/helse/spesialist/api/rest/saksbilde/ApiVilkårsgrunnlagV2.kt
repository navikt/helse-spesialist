@file:UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
    YearMonth::class,
)

package no.nav.helse.spesialist.api.rest.saksbilde

import com.expediagroup.graphql.generator.annotations.GraphQLName
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import kotlinx.serialization.json.JsonClassDiscriminator
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

@GraphQLName("VilkarsgrunnlagV2")
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@Serializable
sealed interface ApiVilkårsgrunnlagV2 {
    val id: UUID
    val inntekter: List<ApiArbeidsgiverinntekt>
    val arbeidsgiverrefusjoner: List<ApiArbeidsgiverrefusjon>
    val skjaeringstidspunkt: LocalDate
    val sykepengegrunnlag: Double
}

@GraphQLName("VilkarsgrunnlagInfotrygdV2")
@Serializable
@SerialName("ApiVilkårsgrunnlagInfotrygdV2")
data class ApiVilkårsgrunnlagInfotrygdV2(
    override val id: UUID,
    override val inntekter: List<ApiArbeidsgiverinntekt>,
    override val arbeidsgiverrefusjoner: List<ApiArbeidsgiverrefusjon>,
    override val skjaeringstidspunkt: LocalDate,
    override val sykepengegrunnlag: Double,
    val omregnetArsinntekt: Double,
    val opptjeningsvurderingId: UUID,
) : ApiVilkårsgrunnlagV2

@GraphQLName("VilkarsgrunnlagSpleisV2")
@Serializable
@SerialName("ApiVilkårsgrunnlagSpleisV2")
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
    val opptjeningsvurderingId: UUID,
    @get:JsonFormat(shape = JsonFormat.Shape.STRING)
    val beregningsgrunnlag: BigDecimal,
    val forsikringsvurderingId: UUID?,
) : ApiVilkårsgrunnlagV2

@GraphQLName("VilkarsgrunnlagAvviksvurdering")
@Serializable
data class ApiVilkårsgrunnlagAvviksvurdering(
    @get:JsonFormat(shape = JsonFormat.Shape.STRING)
    val avviksprosent: BigDecimal,
    @get:JsonFormat(shape = JsonFormat.Shape.STRING)
    val beregningsgrunnlag: BigDecimal,
    @get:JsonFormat(shape = JsonFormat.Shape.STRING)
    val sammenligningsgrunnlag: BigDecimal,
)

@GraphQLName("VilkarsgrunnlagVurdering")
@Serializable
enum class ApiVilkårsgrunnlagVurdering {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_VURDERT,
}

@GraphQLName("Sykepengegrunnlagsgrense")
@Serializable
data class ApiSykepengegrunnlagsgrense(
    val grunnbelop: Int,
    val grense: Int,
    val virkningstidspunkt: LocalDate,
)
