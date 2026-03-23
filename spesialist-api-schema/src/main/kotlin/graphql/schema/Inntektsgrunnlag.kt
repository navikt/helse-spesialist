package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
import java.time.LocalDate
import java.time.YearMonth

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
