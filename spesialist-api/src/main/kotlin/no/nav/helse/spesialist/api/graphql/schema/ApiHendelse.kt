package no.nav.helse.spesialist.api.graphql.schema

import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLHendelse
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLInntektFraAOrdningen
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLInntektsmelding
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSoknadArbeidsgiver
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSoknadArbeidsledig
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSoknadFrilans
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSoknadNav
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSoknadSelvstendig
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSykmelding
import java.util.UUID

fun GraphQLHendelse.tilApiHendelse(): ApiHendelse =
    when (this) {
        is GraphQLInntektsmelding ->
            ApiInntektsmelding(
                id = UUID.fromString(id),
                type = ApiHendelsetype.INNTEKTSMELDING,
                mottattDato = mottattDato,
                beregnetInntekt = beregnetInntekt,
                eksternDokumentId = UUID.fromString(eksternDokumentId),
            )

        is GraphQLSoknadArbeidsgiver ->
            ApiSoknadArbeidsgiver(
                id = UUID.fromString(id),
                type = ApiHendelsetype.SENDT_SOKNAD_ARBEIDSGIVER,
                fom = fom,
                tom = tom,
                rapportertDato = rapportertDato,
                sendtArbeidsgiver = sendtArbeidsgiver,
                eksternDokumentId = UUID.fromString(eksternDokumentId),
            )

        is GraphQLSoknadNav ->
            ApiSoknadNav(
                id = UUID.fromString(id),
                type = ApiHendelsetype.SENDT_SOKNAD_NAV,
                fom = fom,
                tom = tom,
                rapportertDato = rapportertDato,
                sendtNav = sendtNav,
                eksternDokumentId = UUID.fromString(eksternDokumentId),
            )

        is GraphQLSoknadArbeidsledig ->
            ApiSoknadArbeidsledig(
                id = UUID.fromString(id),
                type = ApiHendelsetype.SENDT_SOKNAD_ARBEIDSLEDIG,
                fom = fom,
                tom = tom,
                rapportertDato = rapportertDato,
                sendtNav = sendtNav,
                eksternDokumentId = UUID.fromString(eksternDokumentId),
            )

        is GraphQLSoknadFrilans ->
            ApiSoknadFrilans(
                id = UUID.fromString(id),
                type = ApiHendelsetype.SENDT_SOKNAD_FRILANS,
                fom = fom,
                tom = tom,
                rapportertDato = rapportertDato,
                sendtNav = sendtNav,
                eksternDokumentId = UUID.fromString(eksternDokumentId),
            )

        is GraphQLSoknadSelvstendig ->
            ApiSoknadSelvstendig(
                id = UUID.fromString(id),
                type = ApiHendelsetype.SENDT_SOKNAD_SELVSTENDIG,
                fom = fom,
                tom = tom,
                rapportertDato = rapportertDato,
                sendtNav = sendtNav,
                eksternDokumentId = UUID.fromString(eksternDokumentId),
            )

        is GraphQLSykmelding ->
            ApiSykmelding(
                id = UUID.fromString(id),
                type = ApiHendelsetype.NY_SOKNAD,
                fom = fom,
                tom = tom,
                rapportertDato = rapportertDato,
            )
        is GraphQLInntektFraAOrdningen ->
            ApiInntektHentetFraAOrdningen(
                id = UUID.fromString(id),
                type = ApiHendelsetype.INNTEKT_HENTET_FRA_AORDNINGEN,
                mottattDato = mottattDato,
            )

        else -> throw Exception("Ukjent hendelsestype ${javaClass.name}")
    }
