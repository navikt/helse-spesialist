package no.nav.helse.spleis.graphql.hentsnapshot

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.nav.helse.spleis.graphql.scalars.AnyToLocalDateConverter
import no.nav.helse.spleis.graphql.scalars.AnyToUUIDConverter
import no.nav.helse.spleis.graphql.scalars.LocalDateToAnyConverter
import no.nav.helse.spleis.graphql.scalars.UUIDToAnyConverter
import java.time.LocalDate
import java.util.UUID

@Generated
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "__typename",
    defaultImpl = DefaultGraphQLVilkarsgrunnlagImplementation::class,
)
@JsonSubTypes(
    value = [
        com.fasterxml.jackson.annotation.JsonSubTypes.Type(
            value =
                GraphQLInfotrygdVilkarsgrunnlag::class,
            name = "GraphQLInfotrygdVilkarsgrunnlag",
        ), com.fasterxml.jackson.annotation.JsonSubTypes.Type(
            value =
                GraphQLSpleisVilkarsgrunnlag::class,
            name = "GraphQLSpleisVilkarsgrunnlag",
        ),
    ],
)
public interface GraphQLVilkarsgrunnlag {
    @get:JsonProperty(value = "id")
    public val id: UUID

    @get:JsonProperty(value = "inntekter")
    public val inntekter: List<GraphQLArbeidsgiverinntekt>

    @get:JsonProperty(value = "arbeidsgiverrefusjoner")
    public val arbeidsgiverrefusjoner: List<GraphQLArbeidsgiverrefusjon>

    @get:JsonProperty(value = "omregnetArsinntekt")
    public val omregnetArsinntekt: Double

    @get:JsonProperty(value = "skjaeringstidspunkt")
    public val skjaeringstidspunkt: LocalDate

    @get:JsonProperty(value = "sykepengegrunnlag")
    public val sykepengegrunnlag: Double
}

@Generated
public data class GraphQLInfotrygdVilkarsgrunnlag(
    @JsonSerialize(converter = UUIDToAnyConverter::class)
    @JsonDeserialize(converter = AnyToUUIDConverter::class)
    @get:JsonProperty(value = "id")
    override val id: UUID,
    @get:JsonProperty(value = "inntekter")
    override val inntekter: List<GraphQLArbeidsgiverinntekt>,
    @get:JsonProperty(value = "arbeidsgiverrefusjoner")
    override val arbeidsgiverrefusjoner: List<GraphQLArbeidsgiverrefusjon>,
    @get:JsonProperty(value = "omregnetArsinntekt")
    override val omregnetArsinntekt: Double,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "skjaeringstidspunkt")
    override val skjaeringstidspunkt: LocalDate,
    @get:JsonProperty(value = "sykepengegrunnlag")
    override val sykepengegrunnlag: Double,
) : GraphQLVilkarsgrunnlag

@Generated
public data class GraphQLSpleisVilkarsgrunnlag(
    @JsonSerialize(converter = UUIDToAnyConverter::class)
    @JsonDeserialize(converter = AnyToUUIDConverter::class)
    @get:JsonProperty(value = "id")
    override val id: UUID,
    @get:JsonProperty(value = "inntekter")
    override val inntekter: List<GraphQLArbeidsgiverinntekt>,
    @get:JsonProperty(value = "arbeidsgiverrefusjoner")
    override val arbeidsgiverrefusjoner: List<GraphQLArbeidsgiverrefusjon>,
    @get:JsonProperty(value = "omregnetArsinntekt")
    override val omregnetArsinntekt: Double,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "skjaeringstidspunkt")
    override val skjaeringstidspunkt: LocalDate,
    @get:JsonProperty(value = "sykepengegrunnlag")
    override val sykepengegrunnlag: Double,
    @get:JsonProperty(value = "antallOpptjeningsdagerErMinst")
    public val antallOpptjeningsdagerErMinst: Int,
    @get:JsonProperty(value = "skjonnsmessigFastsattAarlig")
    public val skjonnsmessigFastsattAarlig: Double?,
    @get:JsonProperty(value = "grunnbelop")
    public val grunnbelop: Int,
    @get:JsonProperty(value = "sykepengegrunnlagsgrense")
    public val sykepengegrunnlagsgrense: GraphQLSykepengegrunnlagsgrense,
    @get:JsonProperty(value = "oppfyllerKravOmMedlemskap")
    public val oppfyllerKravOmMedlemskap: Boolean?,
    @get:JsonProperty(value = "oppfyllerKravOmMinstelonn")
    public val oppfyllerKravOmMinstelonn: Boolean,
    @get:JsonProperty(value = "oppfyllerKravOmOpptjening")
    public val oppfyllerKravOmOpptjening: Boolean,
    @JsonSerialize(converter = LocalDateToAnyConverter::class)
    @JsonDeserialize(converter = AnyToLocalDateConverter::class)
    @get:JsonProperty(value = "opptjeningFra")
    public val opptjeningFra: LocalDate,
    @get:JsonProperty(value = "beregningsgrunnlag")
    public val beregningsgrunnlag: Double,
) : GraphQLVilkarsgrunnlag

/**
 * Fallback GraphQLVilkarsgrunnlag implementation that will be used when unknown/unhandled type is
 * encountered.
 */
@Generated
public data class DefaultGraphQLVilkarsgrunnlagImplementation(
    @get:JsonProperty(value = "id")
    override val id: UUID,
    @get:JsonProperty(value = "inntekter")
    override val inntekter: List<GraphQLArbeidsgiverinntekt>,
    @get:JsonProperty(value = "arbeidsgiverrefusjoner")
    override val arbeidsgiverrefusjoner: List<GraphQLArbeidsgiverrefusjon>,
    @get:JsonProperty(value = "omregnetArsinntekt")
    override val omregnetArsinntekt: Double,
    @get:JsonProperty(value = "skjaeringstidspunkt")
    override val skjaeringstidspunkt: LocalDate,
    @get:JsonProperty(value = "sykepengegrunnlag")
    override val sykepengegrunnlag: Double,
) : GraphQLVilkarsgrunnlag
