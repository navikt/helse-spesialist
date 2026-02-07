package no.nav.helse.spleis.graphql.enums

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.`annotation`.JsonEnumDefaultValue
import com.fasterxml.jackson.`annotation`.JsonProperty

@Generated
public enum class GraphQLUtbetalingstatus {
    @JsonProperty("Annullert")
    ANNULLERT,

    @JsonProperty("Forkastet")
    FORKASTET,

    @JsonProperty("Godkjent")
    GODKJENT,

    @JsonProperty("GodkjentUtenUtbetaling")
    GODKJENTUTENUTBETALING,

    @JsonProperty("IkkeGodkjent")
    IKKEGODKJENT,

    @JsonProperty("Overfort")
    OVERFORT,

    @JsonProperty("Sendt")
    SENDT,

    @JsonProperty("Ubetalt")
    UBETALT,

    @JsonProperty("UtbetalingFeilet")
    UTBETALINGFEILET,

    @JsonProperty("Utbetalt")
    UTBETALT,

    /**
     * This is a default enum value that will be used when attempting to deserialize unknown value.
     */
    @JsonEnumDefaultValue
    __UNKNOWN_VALUE,
}
