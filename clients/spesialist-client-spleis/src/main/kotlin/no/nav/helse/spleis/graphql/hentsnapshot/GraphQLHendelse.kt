package no.nav.helse.spleis.graphql.hentsnapshot

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.nav.helse.spleis.graphql.enums.GraphQLHendelsetype
import no.nav.helse.spleis.graphql.scalars.AnyToLocalDateConverter
import no.nav.helse.spleis.graphql.scalars.AnyToLocalDateTimeConverter
import no.nav.helse.spleis.graphql.scalars.LocalDateTimeToAnyConverter
import no.nav.helse.spleis.graphql.scalars.LocalDateToAnyConverter
import java.time.LocalDate
import java.time.LocalDateTime

@Generated
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "__typename",
    defaultImpl = DefaultGraphQLHendelseImplementation::class,
)
@JsonSubTypes(
    value = [
        com.fasterxml.jackson.annotation.JsonSubTypes.Type(
            value =
                GraphQLInntektsmelding::class,
            name = "GraphQLInntektsmelding",
        ), com.fasterxml.jackson.annotation.JsonSubTypes.Type(
            value =
                GraphQLSoknadArbeidsgiver::class,
            name = "GraphQLSoknadArbeidsgiver",
        ), com.fasterxml.jackson.annotation.JsonSubTypes.Type(
            value =
                GraphQLSoknadNav::class,
            name = "GraphQLSoknadNav",
        ), com.fasterxml.jackson.annotation.JsonSubTypes.Type(
            value =
                GraphQLSoknadArbeidsledig::class,
            name = "GraphQLSoknadArbeidsledig",
        ), com.fasterxml.jackson.annotation.JsonSubTypes.Type(
            value =
                GraphQLSoknadFrilans::class,
            name = "GraphQLSoknadFrilans",
        ), com.fasterxml.jackson.annotation.JsonSubTypes.Type(
            value =
                GraphQLSoknadSelvstendig::class,
            name = "GraphQLSoknadSelvstendig",
        ), com.fasterxml.jackson.annotation.JsonSubTypes.Type(
            value =
                GraphQLSykmelding::class,
            name = "GraphQLSykmelding",
        ), com.fasterxml.jackson.annotation.JsonSubTypes.Type(
            value =
                GraphQLInntektFraAOrdningen::class,
            name = "GraphQLInntektFraAOrdningen",
        ),
    ],
)
public interface GraphQLHendelse

@Generated
public data class GraphQLInntektsmelding(
    @get:JsonProperty(value = "beregnetInntekt")
    public val beregnetInntekt: Double,
    @get:JsonProperty(value = "id")
    public val id: String,
    @JsonSerialize(converter = LocalDateTimeToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateTimeConverter::class)
    @get:JsonProperty(value = "mottattDato")
    public val mottattDato: LocalDateTime,
    @get:JsonProperty(value = "type")
    public val type: GraphQLHendelsetype,
    @get:JsonProperty(value = "eksternDokumentId")
    public val eksternDokumentId: String,
) : GraphQLHendelse

@Generated
public data class GraphQLSoknadArbeidsgiver(
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "fom")
    public val fom: LocalDate,
    @get:JsonProperty(value = "id")
    public val id: String,
    @JsonSerialize(converter = LocalDateTimeToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateTimeConverter::class)
    @get:JsonProperty(value = "rapportertDato")
    public val rapportertDato: LocalDateTime,
    @JsonSerialize(converter = LocalDateTimeToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateTimeConverter::class)
    @get:JsonProperty(value = "sendtArbeidsgiver")
    public val sendtArbeidsgiver: LocalDateTime,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "tom")
    public val tom: LocalDate,
    @get:JsonProperty(value = "type")
    public val type: GraphQLHendelsetype,
    @get:JsonProperty(value = "eksternDokumentId")
    public val eksternDokumentId: String,
) : GraphQLHendelse

@Generated
public data class GraphQLSoknadNav(
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "fom")
    public val fom: LocalDate,
    @get:JsonProperty(value = "id")
    public val id: String,
    @JsonSerialize(converter = LocalDateTimeToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateTimeConverter::class)
    @get:JsonProperty(value = "rapportertDato")
    public val rapportertDato: LocalDateTime,
    @JsonSerialize(converter = LocalDateTimeToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateTimeConverter::class)
    @get:JsonProperty(value = "sendtNav")
    public val sendtNav: LocalDateTime,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "tom")
    public val tom: LocalDate,
    @get:JsonProperty(value = "type")
    public val type: GraphQLHendelsetype,
    @get:JsonProperty(value = "eksternDokumentId")
    public val eksternDokumentId: String,
) : GraphQLHendelse

@Generated
public data class GraphQLSoknadArbeidsledig(
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "fom")
    public val fom: LocalDate,
    @get:JsonProperty(value = "id")
    public val id: String,
    @JsonSerialize(converter = LocalDateTimeToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateTimeConverter::class)
    @get:JsonProperty(value = "rapportertDato")
    public val rapportertDato: LocalDateTime,
    @JsonSerialize(converter = LocalDateTimeToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateTimeConverter::class)
    @get:JsonProperty(value = "sendtNav")
    public val sendtNav: LocalDateTime,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "tom")
    public val tom: LocalDate,
    @get:JsonProperty(value = "type")
    public val type: GraphQLHendelsetype,
    @get:JsonProperty(value = "eksternDokumentId")
    public val eksternDokumentId: String,
) : GraphQLHendelse

@Generated
public data class GraphQLSoknadFrilans(
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "fom")
    public val fom: LocalDate,
    @get:JsonProperty(value = "id")
    public val id: String,
    @JsonSerialize(converter = LocalDateTimeToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateTimeConverter::class)
    @get:JsonProperty(value = "rapportertDato")
    public val rapportertDato: LocalDateTime,
    @JsonSerialize(converter = LocalDateTimeToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateTimeConverter::class)
    @get:JsonProperty(value = "sendtNav")
    public val sendtNav: LocalDateTime,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "tom")
    public val tom: LocalDate,
    @get:JsonProperty(value = "type")
    public val type: GraphQLHendelsetype,
    @get:JsonProperty(value = "eksternDokumentId")
    public val eksternDokumentId: String,
) : GraphQLHendelse

@Generated
public data class GraphQLSoknadSelvstendig(
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "fom")
    public val fom: LocalDate,
    @get:JsonProperty(value = "id")
    public val id: String,
    @JsonSerialize(converter = LocalDateTimeToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateTimeConverter::class)
    @get:JsonProperty(value = "rapportertDato")
    public val rapportertDato: LocalDateTime,
    @JsonSerialize(converter = LocalDateTimeToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateTimeConverter::class)
    @get:JsonProperty(value = "sendtNav")
    public val sendtNav: LocalDateTime,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "tom")
    public val tom: LocalDate,
    @get:JsonProperty(value = "type")
    public val type: GraphQLHendelsetype,
    @get:JsonProperty(value = "eksternDokumentId")
    public val eksternDokumentId: String,
) : GraphQLHendelse

@Generated
public data class GraphQLSykmelding(
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "fom")
    public val fom: LocalDate,
    @get:JsonProperty(value = "id")
    public val id: String,
    @JsonSerialize(converter = LocalDateTimeToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateTimeConverter::class)
    @get:JsonProperty(value = "rapportertDato")
    public val rapportertDato: LocalDateTime,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "tom")
    public val tom: LocalDate,
    @get:JsonProperty(value = "type")
    public val type: GraphQLHendelsetype,
) : GraphQLHendelse

@Generated
public data class GraphQLInntektFraAOrdningen(
    @get:JsonProperty(value = "id")
    public val id: String,
    @JsonSerialize(converter = LocalDateTimeToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateTimeConverter::class)
    @get:JsonProperty(value = "mottattDato")
    public val mottattDato: LocalDateTime,
    @get:JsonProperty(value = "type")
    public val type: GraphQLHendelsetype,
    @get:JsonProperty(value = "eksternDokumentId")
    public val eksternDokumentId: String,
) : GraphQLHendelse

/**
 * Fallback GraphQLHendelse implementation that will be used when unknown/unhandled type is
 * encountered.
 */
@Generated
public class DefaultGraphQLHendelseImplementation : GraphQLHendelse
