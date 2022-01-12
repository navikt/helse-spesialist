package no.nav.helse.mediator.api.graphql.schema

import no.nav.helse.mediator.graphql.LocalDate
import no.nav.helse.mediator.graphql.UUID
import no.nav.helse.mediator.graphql.enums.GraphQLSykdomsdagkildetype
import no.nav.helse.mediator.graphql.enums.GraphQLSykdomsdagtype
import no.nav.helse.mediator.graphql.enums.GraphQLUtbetalingsdagType
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLDag
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLSykdomsdagkilde
import no.nav.helse.mediator.graphql.hentsnapshot.GraphQLUtbetalingsinfo

enum class Kildetype {
    INNTEKTSMELDING,
    SAKSBEHANDLER,
    SOKNAD,
    SYKMELDING,
    UKJENT
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
    UBESTEMTDAG,
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

data class Kilde(
    val id: UUID,
    val type: Kildetype
)

data class Utbetalingsinfo(
    val arbeidsgiverbelop: Int?,
    val inntekt: Int?,
    val personbelop: Int?,
    val refusjonsbelop: Int?,
    val totalGrad: Double?,
    val utbetaling: Int?
)

data class Dag(
    val dato: LocalDate,
    val grad: Double?,
    val kilde: Kilde,
    val sykdomsdagtype: Sykdomsdagtype,
    val utbetalingsdagtype: Utbetalingsdagtype,
    val utbetalingsinfo: Utbetalingsinfo?
)

internal fun GraphQLDag.tilDag(): Dag =
    Dag(
        dato = dato,
        grad = grad,
        kilde = kilde.tilKilde(),
        sykdomsdagtype = sykdomsdagtype.tilSykdomsdagtype(),
        utbetalingsdagtype = utbetalingsdagtype.tilUtbetalingsdagtype(),
        utbetalingsinfo = utbetalingsinfo?.tilUtbetalingsinfo()
    )

private fun GraphQLSykdomsdagkilde.tilKilde(): Kilde =
    Kilde(
        id = id,
        type = type.tilKildetype()
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
        GraphQLSykdomsdagtype.FERIEDAG -> Sykdomsdagtype.FERIEDAG
        GraphQLSykdomsdagtype.FORELDETSYKEDAG -> Sykdomsdagtype.FORELDET_SYKEDAG
        GraphQLSykdomsdagtype.FRISKHELGEDAG -> Sykdomsdagtype.FRISK_HELGEDAG
        GraphQLSykdomsdagtype.PERMISJONSDAG -> Sykdomsdagtype.PERMISJONSDAG
        GraphQLSykdomsdagtype.SYKHELGEDAG -> Sykdomsdagtype.SYK_HELGEDAG
        GraphQLSykdomsdagtype.SYKEDAG -> Sykdomsdagtype.SYKEDAG
        GraphQLSykdomsdagtype.UBESTEMTDAG -> Sykdomsdagtype.UBESTEMTDAG
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
        utbetaling = utbetaling
    )
