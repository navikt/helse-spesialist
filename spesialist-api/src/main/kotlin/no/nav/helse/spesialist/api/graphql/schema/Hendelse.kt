package no.nav.helse.spesialist.api.graphql.schema

import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLHendelse
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLInntektsmelding
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSoknadArbeidsgiver
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSoknadArbeidsledig
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSoknadFrilans
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSoknadNav
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSoknadSelvstendig
import no.nav.helse.spleis.graphql.hentsnapshot.GraphQLSykmelding
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class Hendelsetype {
    INNTEKTSMELDING,
    NY_SOKNAD,
    SENDT_SOKNAD_ARBEIDSGIVER,
    SENDT_SOKNAD_NAV,
    SENDT_SOKNAD_ARBEIDSLEDIG,
    SENDT_SOKNAD_FRILANS,
    SENDT_SOKNAD_SELVSTENDIG,
    UKJENT,
}

interface Hendelse {
    val id: UUID
    val type: Hendelsetype
}

data class Inntektsmelding(
    override val id: UUID,
    override val type: Hendelsetype,
    val mottattDato: LocalDateTime,
    val beregnetInntekt: Double,
    val eksternDokumentId: UUID?,
) : Hendelse

data class SoknadArbeidsgiver(
    override val id: UUID,
    override val type: Hendelsetype,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtArbeidsgiver: LocalDateTime,
    val eksternDokumentId: UUID?,
) : Hendelse

data class SoknadNav(
    override val id: UUID,
    override val type: Hendelsetype,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime,
    val eksternDokumentId: UUID?,
) : Hendelse

data class SoknadArbeidsledig(
    override val id: UUID,
    override val type: Hendelsetype,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime,
    val eksternDokumentId: UUID?,
) : Hendelse

data class SoknadFrilans(
    override val id: UUID,
    override val type: Hendelsetype,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime,
    val eksternDokumentId: UUID?,
) : Hendelse

data class SoknadSelvstendig(
    override val id: UUID,
    override val type: Hendelsetype,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime,
    val eksternDokumentId: UUID?,
) : Hendelse

data class Sykmelding(
    override val id: UUID,
    override val type: Hendelsetype,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
) : Hendelse

internal fun GraphQLHendelse.tilHendelse(): Hendelse =
    when (this) {
        is GraphQLInntektsmelding ->
            Inntektsmelding(
                id = UUID.fromString(id),
                type = Hendelsetype.INNTEKTSMELDING,
                mottattDato = mottattDato,
                beregnetInntekt = beregnetInntekt,
                eksternDokumentId = UUID.fromString(eksternDokumentId),
            )

        is GraphQLSoknadArbeidsgiver ->
            SoknadArbeidsgiver(
                id = UUID.fromString(id),
                type = Hendelsetype.SENDT_SOKNAD_ARBEIDSGIVER,
                fom = fom,
                tom = tom,
                rapportertDato = rapportertDato,
                sendtArbeidsgiver = sendtArbeidsgiver,
                eksternDokumentId = UUID.fromString(eksternDokumentId),
            )

        is GraphQLSoknadNav ->
            SoknadNav(
                id = UUID.fromString(id),
                type = Hendelsetype.SENDT_SOKNAD_NAV,
                fom = fom,
                tom = tom,
                rapportertDato = rapportertDato,
                sendtNav = sendtNav,
                eksternDokumentId = UUID.fromString(eksternDokumentId),
            )

        is GraphQLSoknadArbeidsledig ->
            SoknadArbeidsledig(
                id = UUID.fromString(id),
                type = Hendelsetype.SENDT_SOKNAD_ARBEIDSLEDIG,
                fom = fom,
                tom = tom,
                rapportertDato = rapportertDato,
                sendtNav = sendtNav,
                eksternDokumentId = UUID.fromString(eksternDokumentId),
            )

        is GraphQLSoknadFrilans ->
            SoknadFrilans(
                id = UUID.fromString(id),
                type = Hendelsetype.SENDT_SOKNAD_FRILANS,
                fom = fom,
                tom = tom,
                rapportertDato = rapportertDato,
                sendtNav = sendtNav,
                eksternDokumentId = UUID.fromString(eksternDokumentId),
            )

        is GraphQLSoknadSelvstendig ->
            SoknadSelvstendig(
                id = UUID.fromString(id),
                type = Hendelsetype.SENDT_SOKNAD_SELVSTENDIG,
                fom = fom,
                tom = tom,
                rapportertDato = rapportertDato,
                sendtNav = sendtNav,
                eksternDokumentId = UUID.fromString(eksternDokumentId),
            )

        is GraphQLSykmelding ->
            Sykmelding(
                id = UUID.fromString(id),
                type = Hendelsetype.NY_SOKNAD,
                fom = fom,
                tom = tom,
                rapportertDato = rapportertDato,
            )

        else -> throw Exception("Ukjent hendelsestype ${javaClass.name}")
    }
