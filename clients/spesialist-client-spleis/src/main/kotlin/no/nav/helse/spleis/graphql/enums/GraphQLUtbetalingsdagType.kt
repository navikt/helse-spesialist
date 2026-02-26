package no.nav.helse.spleis.graphql.enums

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.`annotation`.JsonEnumDefaultValue
import com.fasterxml.jackson.`annotation`.JsonProperty

@Generated
public enum class GraphQLUtbetalingsdagType {
    @JsonProperty("Arbeidsdag")
    ARBEIDSDAG,

    @JsonProperty("ArbeidsgiverperiodeDag")
    ARBEIDSGIVERPERIODEDAG,

    @JsonProperty("AvvistDag")
    AVVISTDAG,

    @JsonProperty("Feriedag")
    FERIEDAG,

    @JsonProperty("ForeldetDag")
    FORELDETDAG,

    @JsonProperty("Helgedag")
    HELGEDAG,

    @JsonProperty("NavDag")
    NAVDAG,

    @JsonProperty("NavHelgDag")
    NAVHELGDAG,

    @JsonProperty("UkjentDag")
    UKJENTDAG,

    @JsonProperty("Ventetidsdag")
    VENTETIDSDAG,

    /**
     * This is a default enum value that will be used when attempting to deserialize unknown value.
     */
    @JsonEnumDefaultValue
    @Suppress("ktlint:standard:enum-entry-name-case")
    __UNKNOWN_VALUE,
}
