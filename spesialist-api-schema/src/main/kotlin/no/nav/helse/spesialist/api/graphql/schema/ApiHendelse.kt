package no.nav.helse.spesialist.api.graphql.schema

import com.expediagroup.graphql.generator.annotations.GraphQLName
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@GraphQLName("Hendelsetype")
enum class ApiHendelsetype {
    INNTEKTSMELDING,
    INNTEKT_HENTET_FRA_AORDNINGEN,
    NY_SOKNAD,
    SENDT_SOKNAD_ARBEIDSGIVER,
    SENDT_SOKNAD_NAV,
    SENDT_SOKNAD_ARBEIDSLEDIG,
    SENDT_SOKNAD_FRILANS,
    SENDT_SOKNAD_SELVSTENDIG,
    UKJENT,
}

@GraphQLName("Hendelse")
interface ApiHendelse {
    val id: UUID
    val type: ApiHendelsetype
}

@GraphQLName("Inntektsmelding")
data class ApiInntektsmelding(
    override val id: UUID,
    override val type: ApiHendelsetype,
    val mottattDato: LocalDateTime,
    val beregnetInntekt: Double,
    val eksternDokumentId: UUID?,
) : ApiHendelse

@GraphQLName("SoknadArbeidsgiver")
data class ApiSoknadArbeidsgiver(
    override val id: UUID,
    override val type: ApiHendelsetype,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtArbeidsgiver: LocalDateTime,
    val eksternDokumentId: UUID?,
) : ApiHendelse

@GraphQLName("SoknadNav")
data class ApiSoknadNav(
    override val id: UUID,
    override val type: ApiHendelsetype,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime,
    val eksternDokumentId: UUID?,
) : ApiHendelse

@GraphQLName("SoknadArbeidsledig")
data class ApiSoknadArbeidsledig(
    override val id: UUID,
    override val type: ApiHendelsetype,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime,
    val eksternDokumentId: UUID?,
) : ApiHendelse

@GraphQLName("SoknadFrilans")
data class ApiSoknadFrilans(
    override val id: UUID,
    override val type: ApiHendelsetype,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime,
    val eksternDokumentId: UUID?,
) : ApiHendelse

@GraphQLName("SoknadSelvstendig")
data class ApiSoknadSelvstendig(
    override val id: UUID,
    override val type: ApiHendelsetype,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime,
    val eksternDokumentId: UUID?,
) : ApiHendelse

@GraphQLName("Sykmelding")
data class ApiSykmelding(
    override val id: UUID,
    override val type: ApiHendelsetype,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
) : ApiHendelse

@GraphQLName("InntektHentetFraAOrdningen")
data class ApiInntektHentetFraAOrdningen(
    override val id: UUID,
    override val type: ApiHendelsetype,
    val mottattDato: LocalDateTime,
) : ApiHendelse
