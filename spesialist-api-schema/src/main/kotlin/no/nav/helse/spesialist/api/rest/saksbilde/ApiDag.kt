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
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

@GraphQLName("Kildetype")
@Serializable
enum class ApiKildetype {
    INNTEKTSMELDING,
    SAKSBEHANDLER,
    SOKNAD,
    SYKMELDING,
    UKJENT,
}

@GraphQLName("Sykdomsdagtype")
@Serializable
enum class ApiSykdomsdagtype {
    ARBEIDSDAG,
    ARBEIDSGIVERDAG,
    AVSLATT,
    FERIEDAG,
    FORELDET_SYKEDAG,
    FRISK_HELGEDAG,
    PERMISJONSDAG,
    SYK_HELGEDAG,
    SYKEDAG,
    SYKEDAG_NAV,
    UBESTEMTDAG,
    ARBEIDIKKEGJENOPPTATTDAG,
    ANDRE_YTELSER_AAP,
    ANDRE_YTELSER_DAGPENGER,
    ANDRE_YTELSER_FORELDREPENGER,
    ANDRE_YTELSER_OMSORGSPENGER,
    ANDRE_YTELSER_OPPLARINGSPENGER,
    ANDRE_YTELSER_PLEIEPENGER,
    ANDRE_YTELSER_SVANGERSKAPSPENGER,
    MELDINGTILNAVDAG,
    AVSLATTMELDINGTILNAVDAG,
    UKJENT,
}

@GraphQLName("Utbetalingsdagtype")
@Serializable
enum class ApiUtbetalingsdagtype {
    ARBEIDSDAG,
    ARBEIDSGIVERPERIODEDAG,
    AVVIST_DAG,
    FERIEDAG,
    FORELDET_DAG,
    HELGEDAG,
    NAVDAG,
    NAVHELGDAG,
    VENTETIDSDAG,
    UKJENT_DAG,
}

@GraphQLName("Begrunnelse")
@Serializable
enum class ApiBegrunnelse {
    EGENMELDING_UTENFOR_ARBEIDSGIVERPERIODE,
    ETTER_DODSDATO,
    MANGLER_MEDLEMSKAP,
    MANGLER_OPPTJENING,
    MINIMUM_INNTEKT,
    MINIMUM_INNTEKT_OVER_67,
    MINIMUM_SYKDOMSGRAD,
    OVER_70,
    SYKEPENGEDAGER_OPPBRUKT,
    SYKEPENGEDAGER_OPPBRUKT_OVER_67,
    ANDREYTELSER,
    AVSLATT_MELDING_TIL_NAV_DAG,
    MELDING_TIL_NAV_DAG_UTENFOR_VENTETID,
    UKJENT,
}

@GraphQLName("Kilde")
@Serializable
data class ApiKilde(
    val id: UUID,
    val type: ApiKildetype,
)

@GraphQLName("Utbetalingsinfo")
@Serializable
data class ApiUtbetalingsinfo(
    val arbeidsgiverbelop: Int?,
    val inntekt: Int?,
    val personbelop: Int?,
    val refusjonsbelop: Int?,
    val totalGrad: Double?,
    val utbetaling: Int?,
)

@GraphQLName("Dag")
@Serializable
data class ApiDag(
    val dato: LocalDate,
    val grad: Double?,
    val kilde: ApiKilde,
    val sykdomsdagtype: ApiSykdomsdagtype,
    val utbetalingsdagtype: ApiUtbetalingsdagtype,
    val utbetalingsinfo: ApiUtbetalingsinfo?,
    val begrunnelser: List<ApiBegrunnelse>?,
)
