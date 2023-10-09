package no.nav.helse.spesialist.api.graphql.schema

import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLHendelse
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLInntektsmelding
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSoknadArbeidsgiver
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSoknadNav
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSykmelding

enum class Hendelsetype {
    INNTEKTSMELDING,
    NY_SOKNAD,
    SENDT_SOKNAD_ARBEIDSGIVER,
    SENDT_SOKNAD_NAV,
    UKJENT
}

interface Hendelse {
    val id: UUIDString
    val type: Hendelsetype
}

data class Inntektsmelding(
    override val id: UUIDString,
    override val type: Hendelsetype,
    val mottattDato: DateTimeString,
    val beregnetInntekt: Double,
    val eksternDokumentId: UUIDString?
) : Hendelse

data class SoknadArbeidsgiver(
    override val id: UUIDString,
    override val type: Hendelsetype,
    val fom: DateString,
    val tom: DateString,
    val rapportertDato: DateTimeString,
    val sendtArbeidsgiver: DateTimeString,
    val eksternDokumentId: UUIDString?
) : Hendelse

data class SoknadNav(
    override val id: UUIDString,
    override val type: Hendelsetype,
    val fom: DateString,
    val tom: DateString,
    val rapportertDato: DateTimeString,
    val sendtNav: DateTimeString,
    val eksternDokumentId: UUIDString?
) : Hendelse

data class Sykmelding(
    override val id: UUIDString,
    override val type: Hendelsetype,
    val fom: DateString,
    val tom: DateString,
    val rapportertDato: DateTimeString,
) : Hendelse

internal fun GraphQLHendelse.tilHendelse(): Hendelse = when (this) {
    is GraphQLInntektsmelding -> Inntektsmelding(
        id = id,
        type = Hendelsetype.INNTEKTSMELDING,
        mottattDato = mottattDato,
        beregnetInntekt = beregnetInntekt,
        eksternDokumentId = eksternDokumentId,
    )

    is GraphQLSoknadArbeidsgiver -> SoknadArbeidsgiver(
        id = id,
        type = Hendelsetype.SENDT_SOKNAD_ARBEIDSGIVER,
        fom = fom,
        tom = tom,
        rapportertDato = rapportertDato,
        sendtArbeidsgiver = sendtArbeidsgiver,
        eksternDokumentId = eksternDokumentId,
    )

    is GraphQLSoknadNav -> SoknadNav(
        id = id,
        type = Hendelsetype.SENDT_SOKNAD_NAV,
        fom = fom,
        tom = tom,
        rapportertDato = rapportertDato,
        sendtNav = sendtNav,
        eksternDokumentId = eksternDokumentId
    )

    is GraphQLSykmelding -> Sykmelding(
        id = id,
        type = Hendelsetype.NY_SOKNAD,
        fom = fom,
        tom = tom,
        rapportertDato = rapportertDato,
    )

    else -> throw Exception("Ukjent hendelsestype ${javaClass.name}")
}
