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
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import kotlinx.serialization.json.JsonClassDiscriminator
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

@GraphQLName("Hendelsetype")
@Serializable
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
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@Serializable
sealed interface ApiHendelse {
    val id: UUID

    @GraphQLName("type")
    val hendelsetype: ApiHendelsetype
}

@GraphQLName("Inntektsmelding")
@Serializable
@SerialName("ApiInntektsmelding")
data class ApiInntektsmelding(
    override val id: UUID,
    @GraphQLName("type") override val hendelsetype: ApiHendelsetype,
    val mottattDato: LocalDateTime,
    val beregnetInntekt: Double,
    val eksternDokumentId: UUID?,
) : ApiHendelse

@GraphQLName("SoknadArbeidsgiver")
@Serializable
@SerialName("ApiSoknadArbeidsgiver")
data class ApiSoknadArbeidsgiver(
    override val id: UUID,
    @GraphQLName("type") override val hendelsetype: ApiHendelsetype,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtArbeidsgiver: LocalDateTime,
    val eksternDokumentId: UUID?,
) : ApiHendelse

@GraphQLName("SoknadNav")
@Serializable
@SerialName("ApiSoknadNav")
data class ApiSoknadNav(
    override val id: UUID,
    @GraphQLName("type") override val hendelsetype: ApiHendelsetype,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime,
    val eksternDokumentId: UUID?,
) : ApiHendelse

@GraphQLName("SoknadArbeidsledig")
@Serializable
@SerialName("ApiSoknadArbeidsledig")
data class ApiSoknadArbeidsledig(
    override val id: UUID,
    @GraphQLName("type") override val hendelsetype: ApiHendelsetype,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime,
    val eksternDokumentId: UUID?,
) : ApiHendelse

@GraphQLName("SoknadFrilans")
@Serializable
@SerialName("ApiSoknadFrilans")
data class ApiSoknadFrilans(
    override val id: UUID,
    @GraphQLName("type") override val hendelsetype: ApiHendelsetype,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime,
    val eksternDokumentId: UUID?,
) : ApiHendelse

@GraphQLName("SoknadSelvstendig")
@Serializable
@SerialName("ApiSoknadSelvstendig")
data class ApiSoknadSelvstendig(
    override val id: UUID,
    @GraphQLName("type") override val hendelsetype: ApiHendelsetype,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime,
    val eksternDokumentId: UUID?,
) : ApiHendelse

@GraphQLName("Sykmelding")
@Serializable
@SerialName("ApiSykmelding")
data class ApiSykmelding(
    override val id: UUID,
    @GraphQLName("type") override val hendelsetype: ApiHendelsetype,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
) : ApiHendelse

@GraphQLName("InntektHentetFraAOrdningen")
@Serializable
@SerialName("ApiInntektHentetFraAOrdningen")
data class ApiInntektHentetFraAOrdningen(
    override val id: UUID,
    @GraphQLName("type") override val hendelsetype: ApiHendelsetype,
    val mottattDato: LocalDateTime,
) : ApiHendelse
