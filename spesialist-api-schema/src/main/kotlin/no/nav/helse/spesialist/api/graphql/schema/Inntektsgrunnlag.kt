package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@GraphQLName("ArbeidstakerinntektV2")
sealed interface ApiArbeidstakerinntektV2 {
    val arbeidsgiver: String
    val omregnetArsinntekt: ApiArsinntekt?
    val deaktivert: Boolean?
}

@GraphQLName("ArbeidstakerinntektInfotrygd")
data class ApiArbeidstakerinntektInfotrygd(
    override val arbeidsgiver: String,
    override val omregnetArsinntekt: ApiArsinntekt?,
    override val deaktivert: Boolean?,
) : ApiArbeidstakerinntektV2

@GraphQLName("ArbeidstakerinntektSpleisAvventerAvviksvurdering")
data class ApiArbeidstakerinntektSpleisAvventerAvviksvurdering(
    override val arbeidsgiver: String,
    override val omregnetArsinntekt: ApiArsinntekt?,
    override val deaktivert: Boolean?,
    val fom: LocalDate,
    val tom: LocalDate?,
) : ApiArbeidstakerinntektV2

@GraphQLName("ArbeidstakerinntektSpleis")
data class ApiArbeidstakerinntektSpleis(
    override val arbeidsgiver: String,
    override val omregnetArsinntekt: ApiArsinntekt?,
    override val deaktivert: Boolean?,
    val sammenligningsgrunnlag: ApiSammenligningsgrunnlagV2?,
    val skjonnsmessigFastsatt: ApiArsinntekt?,
    val fom: LocalDate?,
    val tom: LocalDate?,
) : ApiArbeidstakerinntektV2

@GraphQLName("SammenligningsgrunnlagV2")
data class ApiSammenligningsgrunnlagV2(
    val belop: BigDecimal,
    val inntektFraAOrdningen: List<ApiInntektFraAOrdningenV2>,
)

@GraphQLName("Arsinntekt")
data class ApiArsinntekt(
    val belop: BigDecimal,
    val inntektFraAOrdningen: List<ApiInntektFraAOrdningenV2>?,
    val kilde: ApiInntektskilde,
    val manedsbelop: BigDecimal,
)

@GraphQLName("InntektFraAOrdningenV2")
data class ApiInntektFraAOrdningenV2(
    val maned: YearMonth,
    val sum: BigDecimal,
)

@GraphQLName("Arbeidsgiverinntekt")
data class ApiArbeidsgiverinntekt(
    val arbeidsgiver: String,
    val omregnetArsinntekt: ApiOmregnetArsinntekt?,
    val sammenligningsgrunnlag: ApiSammenligningsgrunnlag?,
    val skjonnsmessigFastsatt: ApiOmregnetArsinntekt?,
    val deaktivert: Boolean? = null,
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
)

@GraphQLName("Sammenligningsgrunnlag")
data class ApiSammenligningsgrunnlag(
    val belop: Double,
    val inntektFraAOrdningen: List<ApiInntektFraAOrdningen>,
)

@GraphQLName("OmregnetArsinntekt")
data class ApiOmregnetArsinntekt(
    val belop: Double,
    val inntektFraAOrdningen: List<ApiInntektFraAOrdningen>?,
    val kilde: ApiInntektskilde,
    val manedsbelop: Double,
)

@GraphQLName("InntektFraAOrdningen")
data class ApiInntektFraAOrdningen(
    val maned: YearMonth,
    val sum: Double,
)

@GraphQLName("Inntektskilde")
enum class ApiInntektskilde {
    AORDNINGEN,
    INFOTRYGD,
    INNTEKTSMELDING,
    SAKSBEHANDLER,
    IKKE_RAPPORTERT,
    SKJONNSMESSIG_FASTSATT,
}
