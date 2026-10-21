@file:UseContextualSerialization(
    BigDecimal::class,
    Boolean::class,
    LocalDate::class,
    LocalDateTime::class,
    UUID::class,
    YearMonth::class,
)

package no.nav.helse.spesialist.api.rest.saksbilde

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

@Serializable
data class ApiArbeidsgiverinntekt(
    val arbeidsgiver: String,
    val omregnetArsinntekt: ApiOmregnetArsinntekt?,
    val sammenligningsgrunnlag: ApiSammenligningsgrunnlag?,
    val skjonnsmessigFastsatt: ApiOmregnetArsinntekt?,
    val deaktivert: Boolean? = null,
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
)

@Serializable
data class ApiSammenligningsgrunnlag(
    val belop: Double,
    val inntektFraAOrdningen: List<ApiInntektFraAOrdningen>,
)

@Serializable
data class ApiOmregnetArsinntekt(
    val belop: Double,
    val inntektFraAOrdningen: List<ApiInntektFraAOrdningen>?,
    val kilde: ApiInntektskilde,
    val manedsbelop: Double,
)

@Serializable
data class ApiInntektFraAOrdningen(
    val maned: YearMonth,
    val sum: Double,
)

@Serializable
enum class ApiInntektskilde {
    AORDNINGEN,
    INFOTRYGD,
    INNTEKTSMELDING,
    SAKSBEHANDLER,
    IKKE_RAPPORTERT,
    SKJONNSMESSIG_FASTSATT,
}
