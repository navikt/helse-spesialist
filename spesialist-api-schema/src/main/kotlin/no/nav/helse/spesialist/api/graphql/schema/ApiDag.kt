package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
import java.time.LocalDate
import java.util.UUID

@GraphQLName("Kildetype")
enum class ApiKildetype {
    INNTEKTSMELDING,
    SAKSBEHANDLER,
    SOKNAD,
    SYKMELDING,
    UKJENT,
}

@GraphQLName("Sykdomsdagtype")
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
    UKJENT,
}

@GraphQLName("Utbetalingsdagtype")
enum class ApiUtbetalingsdagtype {
    ARBEIDSDAG,
    ARBEIDSGIVERPERIODEDAG,
    AVVIST_DAG,
    FERIEDAG,
    FORELDET_DAG,
    HELGEDAG,
    NAVDAG,
    NAVHELGDAG,
    UKJENT_DAG,
}

@GraphQLName("Begrunnelse")
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
    UKJENT,
}

@GraphQLName("Kilde")
data class ApiKilde(
    val id: UUID,
    val type: ApiKildetype,
)

@GraphQLName("Utbetalingsinfo")
data class ApiUtbetalingsinfo(
    val arbeidsgiverbelop: Int?,
    val inntekt: Int?,
    val personbelop: Int?,
    val refusjonsbelop: Int?,
    val totalGrad: Double?,
    val utbetaling: Int?,
)

@GraphQLName("Dag")
data class ApiDag(
    val dato: LocalDate,
    val grad: Double?,
    val kilde: ApiKilde,
    val sykdomsdagtype: ApiSykdomsdagtype,
    val utbetalingsdagtype: ApiUtbetalingsdagtype,
    val utbetalingsinfo: ApiUtbetalingsinfo?,
    val begrunnelser: List<ApiBegrunnelse>?,
)
