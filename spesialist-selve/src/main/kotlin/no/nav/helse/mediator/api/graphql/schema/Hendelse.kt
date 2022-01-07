package no.nav.helse.mediator.api.graphql.schema

import no.nav.helse.mediator.graphql.LocalDate
import no.nav.helse.mediator.graphql.LocalDateTime
import no.nav.helse.mediator.graphql.UUID
import no.nav.helse.mediator.graphql.hentsnapshot.*

enum class Hendelsetype {
    INNTEKTSMELDING,
    NY_SOKNAD,
    SENDT_SOKNAD_ARBEIDSGIVER,
    SENDT_SOKNAD_NAV,
    UKJENT
}

interface Hendelse {
    val id: UUID
    val type: Hendelsetype
}

data class Inntektsmelding(
    override val id: UUID,
    override val type: Hendelsetype,
    val mottattDato: LocalDateTime,
    val beregnetInntekt: Double
) : Hendelse

data class SoknadArbeidsgiver(
    override val id: UUID,
    override val type: Hendelsetype,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtArbeidsgiver: LocalDateTime
) : Hendelse

data class SoknadNav(
    override val id: UUID,
    override val type: Hendelsetype,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime
) : Hendelse

data class Sykmelding(
    override val id: UUID,
    override val type: Hendelsetype,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
) : Hendelse

internal fun GraphQLHendelse.tilHendelse(): Hendelse = when (this) {
    is GraphQLInntektsmelding -> Inntektsmelding(
        id = id,
        type = Hendelsetype.INNTEKTSMELDING,
        mottattDato = mottattDato,
        beregnetInntekt = beregnetInntekt
    )
    is GraphQLSoknadArbeidsgiver -> SoknadArbeidsgiver(
        id = id,
        type = Hendelsetype.SENDT_SOKNAD_ARBEIDSGIVER,
        fom = fom,
        tom = tom,
        rapportertDato = rapportertDato,
        sendtArbeidsgiver = sendtArbeidsgiver
    )
    is GraphQLSoknadNav -> SoknadNav(
        id = id,
        type = Hendelsetype.SENDT_SOKNAD_NAV,
        fom = fom,
        tom = tom,
        rapportertDato = rapportertDato,
        sendtNav = sendtNav
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
