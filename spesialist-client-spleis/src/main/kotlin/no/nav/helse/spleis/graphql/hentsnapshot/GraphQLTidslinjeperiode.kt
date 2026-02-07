package no.nav.helse.spleis.graphql.hentsnapshot

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.nav.helse.spleis.graphql.enums.GraphQLInntektstype
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetilstand
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetype
import no.nav.helse.spleis.graphql.scalars.AnyToLocalDateConverter
import no.nav.helse.spleis.graphql.scalars.AnyToLocalDateTimeConverter
import no.nav.helse.spleis.graphql.scalars.AnyToUUIDConverter
import no.nav.helse.spleis.graphql.scalars.LocalDateTimeToAnyConverter
import no.nav.helse.spleis.graphql.scalars.LocalDateToAnyConverter
import no.nav.helse.spleis.graphql.scalars.UUIDToAnyConverter
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.spleis.graphql.enums.GraphQLInntektstype.__UNKNOWN_VALUE as graphQLInntektstype__UNKNOWN_VALUE
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetilstand.__UNKNOWN_VALUE as graphQLPeriodetilstand__UNKNOWN_VALUE
import no.nav.helse.spleis.graphql.enums.GraphQLPeriodetype.__UNKNOWN_VALUE as graphQLPeriodetype__UNKNOWN_VALUE

@Generated
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "__typename",
    defaultImpl = DefaultGraphQLTidslinjeperiodeImplementation::class,
)
@JsonSubTypes(
    value = [
        com.fasterxml.jackson.annotation.JsonSubTypes.Type(
            value =
                GraphQLUberegnetPeriode::class,
            name = "GraphQLUberegnetPeriode",
        ), com.fasterxml.jackson.annotation.JsonSubTypes.Type(
            value =
                GraphQLBeregnetPeriode::class,
            name = "GraphQLBeregnetPeriode",
        ),
    ],
)
public interface GraphQLTidslinjeperiode {
    @get:JsonProperty(value = "behandlingId")
    public val behandlingId: UUID

    @get:JsonProperty(value = "erForkastet")
    public val erForkastet: Boolean

    @get:JsonProperty(value = "fom")
    public val fom: LocalDate

    @get:JsonProperty(value = "tom")
    public val tom: LocalDate

    @get:JsonProperty(value = "inntektstype")
    public val inntektstype: GraphQLInntektstype

    @get:JsonProperty(value = "opprettet")
    public val opprettet: LocalDateTime

    @get:JsonProperty(value = "periodetype")
    public val periodetype: GraphQLPeriodetype

    @get:JsonProperty(value = "periodetilstand")
    public val periodetilstand: GraphQLPeriodetilstand

    @get:JsonProperty(value = "skjaeringstidspunkt")
    public val skjaeringstidspunkt: LocalDate

    @get:JsonProperty(value = "tidslinje")
    public val tidslinje: List<GraphQLDag>

    @get:JsonProperty(value = "hendelser")
    public val hendelser: List<GraphQLHendelse>

    @get:JsonProperty(value = "vedtaksperiodeId")
    public val vedtaksperiodeId: UUID
}

@Generated
public data class GraphQLUberegnetPeriode(
    @JsonSerialize(converter = UUIDToAnyConverter::class)
    @JsonDeserialize(converter = AnyToUUIDConverter::class)
    @get:JsonProperty(value = "behandlingId")
    override val behandlingId: UUID,
    @get:JsonProperty(value = "erForkastet")
    override val erForkastet: Boolean,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "fom")
    override val fom: LocalDate,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "tom")
    override val tom: LocalDate,
    @get:JsonProperty(value = "inntektstype")
    override val inntektstype: GraphQLInntektstype,
    @JsonSerialize(converter = LocalDateTimeToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateTimeConverter::class)
    @get:JsonProperty(value = "opprettet")
    override val opprettet: LocalDateTime,
    @get:JsonProperty(value = "periodetype")
    override val periodetype: GraphQLPeriodetype,
    @get:JsonProperty(value = "periodetilstand")
    override val periodetilstand: GraphQLPeriodetilstand,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "skjaeringstidspunkt")
    override val skjaeringstidspunkt: LocalDate,
    @get:JsonProperty(value = "tidslinje")
    override val tidslinje: List<GraphQLDag>,
    @get:JsonProperty(value = "hendelser")
    override val hendelser: List<GraphQLHendelse>,
    @JsonSerialize(converter = UUIDToAnyConverter::class)
    @JsonDeserialize(converter = AnyToUUIDConverter::class)
    @get:JsonProperty(value = "vedtaksperiodeId")
    override val vedtaksperiodeId: UUID,
) : GraphQLTidslinjeperiode

@Generated
public data class GraphQLBeregnetPeriode(
    @JsonSerialize(converter = UUIDToAnyConverter::class)
    @JsonDeserialize(converter = AnyToUUIDConverter::class)
    @get:JsonProperty(value = "behandlingId")
    override val behandlingId: UUID,
    @get:JsonProperty(value = "erForkastet")
    override val erForkastet: Boolean,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "fom")
    override val fom: LocalDate,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "tom")
    override val tom: LocalDate,
    @get:JsonProperty(value = "inntektstype")
    override val inntektstype: GraphQLInntektstype,
    @JsonSerialize(converter = LocalDateTimeToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateTimeConverter::class)
    @get:JsonProperty(value = "opprettet")
    override val opprettet: LocalDateTime,
    @get:JsonProperty(value = "periodetype")
    override val periodetype: GraphQLPeriodetype,
    @get:JsonProperty(value = "periodetilstand")
    override val periodetilstand: GraphQLPeriodetilstand,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "skjaeringstidspunkt")
    override val skjaeringstidspunkt: LocalDate,
    @get:JsonProperty(value = "tidslinje")
    override val tidslinje: List<GraphQLDag>,
    @get:JsonProperty(value = "hendelser")
    override val hendelser: List<GraphQLHendelse>,
    @JsonSerialize(converter = UUIDToAnyConverter::class)
    @JsonDeserialize(converter = AnyToUUIDConverter::class)
    @get:JsonProperty(value = "vedtaksperiodeId")
    override val vedtaksperiodeId: UUID,
    @JsonSerialize(converter = UUIDToAnyConverter::class)
    @JsonDeserialize(converter = AnyToUUIDConverter::class)
    @get:JsonProperty(value = "beregningId")
    public val beregningId: UUID,
    @get:JsonProperty(value = "forbrukteSykedager")
    public val forbrukteSykedager: Int?,
    @get:JsonProperty(value = "gjenstaendeSykedager")
    public val gjenstaendeSykedager: Int?,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "maksdato")
    public val maksdato: LocalDate,
    @get:JsonProperty(value = "pensjonsgivendeInntekter")
    public val pensjonsgivendeInntekter: List<GraphQLPensjonsgivendeInntekt>,
    @get:JsonProperty(value = "annulleringskandidater")
    public val annulleringskandidater: List<GraphQLAnnulleringskandidat>,
    @get:JsonProperty(value = "periodevilkar")
    public val periodevilkar: GraphQLPeriodevilkar,
    @get:JsonProperty(value = "utbetaling")
    public val utbetaling: GraphQLUtbetaling,
    @JsonSerialize(converter = UUIDToAnyConverter::class)
    @JsonDeserialize(converter = AnyToUUIDConverter::class)
    @get:JsonProperty(value = "vilkarsgrunnlagId")
    public val vilkarsgrunnlagId: UUID?,
) : GraphQLTidslinjeperiode

/**
 * Fallback GraphQLTidslinjeperiode implementation that will be used when unknown/unhandled type is
 * encountered.
 */
@Generated
public data class DefaultGraphQLTidslinjeperiodeImplementation(
    @get:JsonProperty(value = "behandlingId")
    override val behandlingId: UUID,
    @get:JsonProperty(value = "erForkastet")
    override val erForkastet: Boolean,
    @get:JsonProperty(value = "fom")
    override val fom: LocalDate,
    @get:JsonProperty(value = "tom")
    override val tom: LocalDate,
    @get:JsonProperty(value = "inntektstype")
    override val inntektstype: GraphQLInntektstype = graphQLInntektstype__UNKNOWN_VALUE,
    @get:JsonProperty(value = "opprettet")
    override val opprettet: LocalDateTime,
    @get:JsonProperty(value = "periodetype")
    override val periodetype: GraphQLPeriodetype = graphQLPeriodetype__UNKNOWN_VALUE,
    @get:JsonProperty(value = "periodetilstand")
    override val periodetilstand: GraphQLPeriodetilstand = graphQLPeriodetilstand__UNKNOWN_VALUE,
    @get:JsonProperty(value = "skjaeringstidspunkt")
    override val skjaeringstidspunkt: LocalDate,
    @get:JsonProperty(value = "tidslinje")
    override val tidslinje: List<GraphQLDag>,
    @get:JsonProperty(value = "hendelser")
    override val hendelser: List<GraphQLHendelse>,
    @get:JsonProperty(value = "vedtaksperiodeId")
    override val vedtaksperiodeId: UUID,
) : GraphQLTidslinjeperiode
