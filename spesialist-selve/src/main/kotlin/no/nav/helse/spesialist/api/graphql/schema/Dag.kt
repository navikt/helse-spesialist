package no.nav.helse.spesialist.api.graphql.schema

import no.nav.helse.spleis.graphql.enums.GraphQLBegrunnelse
import no.nav.helse.spleis.graphql.enums.GraphQLSykdomsdagkildetype
import no.nav.helse.spleis.graphql.enums.GraphQLSykdomsdagtype
import no.nav.helse.spleis.graphql.enums.GraphQLUtbetalingsdagType
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLDag
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSykdomsdagkilde
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLUtbetalingsinfo
import java.time.LocalDate
import java.util.UUID

enum class Kildetype {
    INNTEKTSMELDING,
    SAKSBEHANDLER,
    SOKNAD,
    SYKMELDING,
    UKJENT,
}

enum class Sykdomsdagtype {
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
}

enum class Utbetalingsdagtype {
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

enum class Begrunnelse {
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

data class Kilde(
    val id: UUID,
    val type: Kildetype,
)

data class Utbetalingsinfo(
    val arbeidsgiverbelop: Int?,
    val inntekt: Int?,
    val personbelop: Int?,
    val refusjonsbelop: Int?,
    val totalGrad: Double?,
    val utbetaling: Int?,
)

data class Dag(
    val dato: LocalDate,
    val grad: Double?,
    val kilde: Kilde,
    val sykdomsdagtype: Sykdomsdagtype,
    val utbetalingsdagtype: Utbetalingsdagtype,
    val utbetalingsinfo: Utbetalingsinfo?,
    val begrunnelser: List<Begrunnelse>?,
)

internal fun GraphQLDag.tilDag(): Dag =
    Dag(
        dato = dato,
        grad = grad,
        kilde = kilde.tilKilde(),
        sykdomsdagtype = sykdomsdagtype.tilSykdomsdagtype(),
        utbetalingsdagtype = utbetalingsdagtype.tilUtbetalingsdagtype(),
        utbetalingsinfo = utbetalingsinfo?.tilUtbetalingsinfo(),
        begrunnelser = begrunnelser?.map { it.tilBegrunnelse() },
    )

private fun GraphQLSykdomsdagkilde.tilKilde(): Kilde =
    Kilde(
        id = id,
        type = type.tilKildetype(),
    )

private fun GraphQLSykdomsdagkildetype.tilKildetype(): Kildetype =
    when (this) {
        GraphQLSykdomsdagkildetype.INNTEKTSMELDING -> Kildetype.INNTEKTSMELDING
        GraphQLSykdomsdagkildetype.SAKSBEHANDLER -> Kildetype.SAKSBEHANDLER
        GraphQLSykdomsdagkildetype.SOKNAD -> Kildetype.SOKNAD
        GraphQLSykdomsdagkildetype.SYKMELDING -> Kildetype.SYKMELDING
        else -> Kildetype.UKJENT
    }

private fun GraphQLSykdomsdagtype.tilSykdomsdagtype(): Sykdomsdagtype =
    when (this) {
        GraphQLSykdomsdagtype.ARBEIDSDAG -> Sykdomsdagtype.ARBEIDSDAG
        GraphQLSykdomsdagtype.ARBEIDSGIVERDAG -> Sykdomsdagtype.ARBEIDSGIVERDAG
        GraphQLSykdomsdagtype.AVSLATT -> Sykdomsdagtype.AVSLATT
        GraphQLSykdomsdagtype.ARBEIDIKKEGJENOPPTATTDAG -> Sykdomsdagtype.ARBEIDIKKEGJENOPPTATTDAG
        GraphQLSykdomsdagtype.FERIEDAG -> Sykdomsdagtype.FERIEDAG
        GraphQLSykdomsdagtype.FORELDETSYKEDAG -> Sykdomsdagtype.FORELDET_SYKEDAG
        GraphQLSykdomsdagtype.FRISKHELGEDAG -> Sykdomsdagtype.FRISK_HELGEDAG
        GraphQLSykdomsdagtype.PERMISJONSDAG -> Sykdomsdagtype.PERMISJONSDAG
        GraphQLSykdomsdagtype.SYKHELGEDAG -> Sykdomsdagtype.SYK_HELGEDAG
        GraphQLSykdomsdagtype.SYKEDAG -> Sykdomsdagtype.SYKEDAG
        GraphQLSykdomsdagtype.SYKEDAGNAV -> Sykdomsdagtype.SYKEDAG_NAV
        GraphQLSykdomsdagtype.UBESTEMTDAG -> Sykdomsdagtype.UBESTEMTDAG
        GraphQLSykdomsdagtype.ANDREYTELSERAAP -> Sykdomsdagtype.ANDRE_YTELSER_AAP
        GraphQLSykdomsdagtype.ANDREYTELSERDAGPENGER -> Sykdomsdagtype.ANDRE_YTELSER_DAGPENGER
        GraphQLSykdomsdagtype.ANDREYTELSERFORELDREPENGER -> Sykdomsdagtype.ANDRE_YTELSER_FORELDREPENGER
        GraphQLSykdomsdagtype.ANDREYTELSEROMSORGSPENGER -> Sykdomsdagtype.ANDRE_YTELSER_OMSORGSPENGER
        GraphQLSykdomsdagtype.ANDREYTELSEROPPLARINGSPENGER -> Sykdomsdagtype.ANDRE_YTELSER_OPPLARINGSPENGER
        GraphQLSykdomsdagtype.ANDREYTELSERPLEIEPENGER -> Sykdomsdagtype.ANDRE_YTELSER_PLEIEPENGER
        GraphQLSykdomsdagtype.ANDREYTELSERSVANGERSKAPSPENGER -> Sykdomsdagtype.ANDRE_YTELSER_SVANGERSKAPSPENGER
        else -> throw Exception("Ukjent sykdomsdagtype $name")
    }

private fun GraphQLUtbetalingsdagType.tilUtbetalingsdagtype(): Utbetalingsdagtype =
    when (this) {
        GraphQLUtbetalingsdagType.ARBEIDSDAG -> Utbetalingsdagtype.ARBEIDSDAG
        GraphQLUtbetalingsdagType.ARBEIDSGIVERPERIODEDAG -> Utbetalingsdagtype.ARBEIDSGIVERPERIODEDAG
        GraphQLUtbetalingsdagType.AVVISTDAG -> Utbetalingsdagtype.AVVIST_DAG
        GraphQLUtbetalingsdagType.FERIEDAG -> Utbetalingsdagtype.FERIEDAG
        GraphQLUtbetalingsdagType.FORELDETDAG -> Utbetalingsdagtype.FORELDET_DAG
        GraphQLUtbetalingsdagType.HELGEDAG -> Utbetalingsdagtype.HELGEDAG
        GraphQLUtbetalingsdagType.NAVDAG -> Utbetalingsdagtype.NAVDAG
        GraphQLUtbetalingsdagType.NAVHELGDAG -> Utbetalingsdagtype.NAVHELGDAG
        else -> Utbetalingsdagtype.UKJENT_DAG
    }

private fun GraphQLUtbetalingsinfo.tilUtbetalingsinfo(): Utbetalingsinfo =
    Utbetalingsinfo(
        arbeidsgiverbelop = arbeidsgiverbelop,
        inntekt = inntekt,
        personbelop = personbelop,
        refusjonsbelop = refusjonsbelop,
        totalGrad = totalGrad,
        utbetaling = utbetaling,
    )

private fun GraphQLBegrunnelse.tilBegrunnelse(): Begrunnelse =
    when (this) {
        GraphQLBegrunnelse.EGENMELDINGUTENFORARBEIDSGIVERPERIODE -> Begrunnelse.EGENMELDING_UTENFOR_ARBEIDSGIVERPERIODE
        GraphQLBegrunnelse.ETTERDODSDATO -> Begrunnelse.ETTER_DODSDATO
        GraphQLBegrunnelse.MANGLERMEDLEMSKAP -> Begrunnelse.MANGLER_MEDLEMSKAP
        GraphQLBegrunnelse.MANGLEROPPTJENING -> Begrunnelse.MANGLER_OPPTJENING
        GraphQLBegrunnelse.MINIMUMINNTEKT -> Begrunnelse.MINIMUM_INNTEKT
        GraphQLBegrunnelse.MINIMUMINNTEKTOVER67 -> Begrunnelse.MINIMUM_INNTEKT_OVER_67
        GraphQLBegrunnelse.MINIMUMSYKDOMSGRAD -> Begrunnelse.MINIMUM_SYKDOMSGRAD
        GraphQLBegrunnelse.OVER70 -> Begrunnelse.OVER_70
        GraphQLBegrunnelse.SYKEPENGEDAGEROPPBRUKT -> Begrunnelse.SYKEPENGEDAGER_OPPBRUKT
        GraphQLBegrunnelse.SYKEPENGEDAGEROPPBRUKTOVER67 -> Begrunnelse.SYKEPENGEDAGER_OPPBRUKT_OVER_67
        GraphQLBegrunnelse.ANDREYTELSER -> Begrunnelse.ANDREYTELSER
        GraphQLBegrunnelse.__UNKNOWN_VALUE -> Begrunnelse.UKJENT
    }
