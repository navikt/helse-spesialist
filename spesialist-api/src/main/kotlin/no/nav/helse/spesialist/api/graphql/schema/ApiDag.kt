package no.nav.helse.spesialist.api.graphql.schema

import no.nav.helse.spleis.graphql.enums.GraphQLBegrunnelse
import no.nav.helse.spleis.graphql.enums.GraphQLSykdomsdagkildetype
import no.nav.helse.spleis.graphql.enums.GraphQLSykdomsdagtype
import no.nav.helse.spleis.graphql.enums.GraphQLUtbetalingsdagType
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLDag
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSykdomsdagkilde
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLUtbetalingsinfo

fun GraphQLDag.tilDag(): ApiDag =
    ApiDag(
        dato = dato,
        grad = grad,
        kilde = kilde.tilKilde(),
        sykdomsdagtype = sykdomsdagtype.tilSykdomsdagtype(),
        utbetalingsdagtype = utbetalingsdagtype.tilUtbetalingsdagtype(),
        utbetalingsinfo = utbetalingsinfo?.tilUtbetalingsinfo(),
        begrunnelser = begrunnelser?.map { it.tilBegrunnelse() },
    )

private fun GraphQLSykdomsdagkilde.tilKilde(): ApiKilde =
    ApiKilde(
        id = id,
        type = type.tilKildetype(),
    )

private fun GraphQLSykdomsdagkildetype.tilKildetype(): ApiKildetype =
    when (this) {
        GraphQLSykdomsdagkildetype.INNTEKTSMELDING -> ApiKildetype.INNTEKTSMELDING
        GraphQLSykdomsdagkildetype.SAKSBEHANDLER -> ApiKildetype.SAKSBEHANDLER
        GraphQLSykdomsdagkildetype.SOKNAD -> ApiKildetype.SOKNAD
        GraphQLSykdomsdagkildetype.SYKMELDING -> ApiKildetype.SYKMELDING
        else -> ApiKildetype.UKJENT
    }

private fun GraphQLSykdomsdagtype.tilSykdomsdagtype(): ApiSykdomsdagtype =
    when (this) {
        GraphQLSykdomsdagtype.ARBEIDSDAG -> ApiSykdomsdagtype.ARBEIDSDAG
        GraphQLSykdomsdagtype.ARBEIDSGIVERDAG -> ApiSykdomsdagtype.ARBEIDSGIVERDAG
        GraphQLSykdomsdagtype.AVSLATT -> ApiSykdomsdagtype.AVSLATT
        GraphQLSykdomsdagtype.ARBEIDIKKEGJENOPPTATTDAG -> ApiSykdomsdagtype.ARBEIDIKKEGJENOPPTATTDAG
        GraphQLSykdomsdagtype.FERIEDAG -> ApiSykdomsdagtype.FERIEDAG
        GraphQLSykdomsdagtype.FORELDETSYKEDAG -> ApiSykdomsdagtype.FORELDET_SYKEDAG
        GraphQLSykdomsdagtype.FRISKHELGEDAG -> ApiSykdomsdagtype.FRISK_HELGEDAG
        GraphQLSykdomsdagtype.PERMISJONSDAG -> ApiSykdomsdagtype.PERMISJONSDAG
        GraphQLSykdomsdagtype.SYKHELGEDAG -> ApiSykdomsdagtype.SYK_HELGEDAG
        GraphQLSykdomsdagtype.SYKEDAG -> ApiSykdomsdagtype.SYKEDAG
        GraphQLSykdomsdagtype.SYKEDAGNAV -> ApiSykdomsdagtype.SYKEDAG_NAV
        GraphQLSykdomsdagtype.UBESTEMTDAG -> ApiSykdomsdagtype.UBESTEMTDAG
        GraphQLSykdomsdagtype.ANDREYTELSERAAP -> ApiSykdomsdagtype.ANDRE_YTELSER_AAP
        GraphQLSykdomsdagtype.ANDREYTELSERDAGPENGER -> ApiSykdomsdagtype.ANDRE_YTELSER_DAGPENGER
        GraphQLSykdomsdagtype.ANDREYTELSERFORELDREPENGER -> ApiSykdomsdagtype.ANDRE_YTELSER_FORELDREPENGER
        GraphQLSykdomsdagtype.ANDREYTELSEROMSORGSPENGER -> ApiSykdomsdagtype.ANDRE_YTELSER_OMSORGSPENGER
        GraphQLSykdomsdagtype.ANDREYTELSEROPPLARINGSPENGER -> ApiSykdomsdagtype.ANDRE_YTELSER_OPPLARINGSPENGER
        GraphQLSykdomsdagtype.ANDREYTELSERPLEIEPENGER -> ApiSykdomsdagtype.ANDRE_YTELSER_PLEIEPENGER
        GraphQLSykdomsdagtype.ANDREYTELSERSVANGERSKAPSPENGER -> ApiSykdomsdagtype.ANDRE_YTELSER_SVANGERSKAPSPENGER
        else -> throw Exception("Ukjent sykdomsdagtype $name")
    }

private fun GraphQLUtbetalingsdagType.tilUtbetalingsdagtype(): ApiUtbetalingsdagtype =
    when (this) {
        GraphQLUtbetalingsdagType.ARBEIDSDAG -> ApiUtbetalingsdagtype.ARBEIDSDAG
        GraphQLUtbetalingsdagType.ARBEIDSGIVERPERIODEDAG -> ApiUtbetalingsdagtype.ARBEIDSGIVERPERIODEDAG
        GraphQLUtbetalingsdagType.AVVISTDAG -> ApiUtbetalingsdagtype.AVVIST_DAG
        GraphQLUtbetalingsdagType.FERIEDAG -> ApiUtbetalingsdagtype.FERIEDAG
        GraphQLUtbetalingsdagType.FORELDETDAG -> ApiUtbetalingsdagtype.FORELDET_DAG
        GraphQLUtbetalingsdagType.HELGEDAG -> ApiUtbetalingsdagtype.HELGEDAG
        GraphQLUtbetalingsdagType.NAVDAG -> ApiUtbetalingsdagtype.NAVDAG
        GraphQLUtbetalingsdagType.NAVHELGDAG -> ApiUtbetalingsdagtype.NAVHELGDAG
        else -> ApiUtbetalingsdagtype.UKJENT_DAG
    }

private fun GraphQLUtbetalingsinfo.tilUtbetalingsinfo(): ApiUtbetalingsinfo =
    ApiUtbetalingsinfo(
        arbeidsgiverbelop = arbeidsgiverbelop,
        inntekt = inntekt,
        personbelop = personbelop,
        refusjonsbelop = refusjonsbelop,
        totalGrad = totalGrad,
        utbetaling = utbetaling,
    )

private fun GraphQLBegrunnelse.tilBegrunnelse(): ApiBegrunnelse =
    when (this) {
        GraphQLBegrunnelse.EGENMELDINGUTENFORARBEIDSGIVERPERIODE -> ApiBegrunnelse.EGENMELDING_UTENFOR_ARBEIDSGIVERPERIODE
        GraphQLBegrunnelse.ETTERDODSDATO -> ApiBegrunnelse.ETTER_DODSDATO
        GraphQLBegrunnelse.MANGLERMEDLEMSKAP -> ApiBegrunnelse.MANGLER_MEDLEMSKAP
        GraphQLBegrunnelse.MANGLEROPPTJENING -> ApiBegrunnelse.MANGLER_OPPTJENING
        GraphQLBegrunnelse.MINIMUMINNTEKT -> ApiBegrunnelse.MINIMUM_INNTEKT
        GraphQLBegrunnelse.MINIMUMINNTEKTOVER67 -> ApiBegrunnelse.MINIMUM_INNTEKT_OVER_67
        GraphQLBegrunnelse.MINIMUMSYKDOMSGRAD -> ApiBegrunnelse.MINIMUM_SYKDOMSGRAD
        GraphQLBegrunnelse.OVER70 -> ApiBegrunnelse.OVER_70
        GraphQLBegrunnelse.SYKEPENGEDAGEROPPBRUKT -> ApiBegrunnelse.SYKEPENGEDAGER_OPPBRUKT
        GraphQLBegrunnelse.SYKEPENGEDAGEROPPBRUKTOVER67 -> ApiBegrunnelse.SYKEPENGEDAGER_OPPBRUKT_OVER_67
        GraphQLBegrunnelse.ANDREYTELSER -> ApiBegrunnelse.ANDREYTELSER
        GraphQLBegrunnelse.__UNKNOWN_VALUE -> ApiBegrunnelse.UKJENT
    }
