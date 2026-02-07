package no.nav.helse.spleis.graphql.enums

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.`annotation`.JsonEnumDefaultValue
import com.fasterxml.jackson.`annotation`.JsonProperty

@Generated
public enum class GraphQLSykdomsdagkildetype {
    @JsonProperty("Inntektsmelding")
    INNTEKTSMELDING,

    @JsonProperty("Saksbehandler")
    SAKSBEHANDLER,

    @JsonProperty("Soknad")
    SOKNAD,

    @JsonProperty("Sykmelding")
    SYKMELDING,

    @JsonProperty("Ukjent")
    UKJENT,

    /**
     * This is a default enum value that will be used when attempting to deserialize unknown value.
     */
    @JsonEnumDefaultValue
    __UNKNOWN_VALUE,
}
