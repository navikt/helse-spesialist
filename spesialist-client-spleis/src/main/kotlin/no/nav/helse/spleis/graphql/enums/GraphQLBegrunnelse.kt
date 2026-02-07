package no.nav.helse.spleis.graphql.enums

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.`annotation`.JsonEnumDefaultValue
import com.fasterxml.jackson.`annotation`.JsonProperty

@Generated
public enum class GraphQLBegrunnelse {
    @JsonProperty("AndreYtelser")
    ANDREYTELSER,

    @JsonProperty("EgenmeldingUtenforArbeidsgiverperiode")
    EGENMELDINGUTENFORARBEIDSGIVERPERIODE,

    @JsonProperty("EtterDodsdato")
    ETTERDODSDATO,

    @JsonProperty("ManglerMedlemskap")
    MANGLERMEDLEMSKAP,

    @JsonProperty("ManglerOpptjening")
    MANGLEROPPTJENING,

    @JsonProperty("MinimumInntekt")
    MINIMUMINNTEKT,

    @JsonProperty("MinimumInntektOver67")
    MINIMUMINNTEKTOVER67,

    @JsonProperty("MinimumSykdomsgrad")
    MINIMUMSYKDOMSGRAD,

    @JsonProperty("Over70")
    OVER70,

    @JsonProperty("SykepengedagerOppbrukt")
    SYKEPENGEDAGEROPPBRUKT,

    @JsonProperty("SykepengedagerOppbruktOver67")
    SYKEPENGEDAGEROPPBRUKTOVER67,

    /**
     * This is a default enum value that will be used when attempting to deserialize unknown value.
     */
    @JsonEnumDefaultValue
    @Suppress("ktlint:standard:enum-entry-name-case")
    __UNKNOWN_VALUE,
}
