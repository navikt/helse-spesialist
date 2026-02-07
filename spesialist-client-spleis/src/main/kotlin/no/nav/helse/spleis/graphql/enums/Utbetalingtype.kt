package no.nav.helse.spleis.graphql.enums

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.`annotation`.JsonEnumDefaultValue

@Generated
public enum class Utbetalingtype {
    ANNULLERING,
    ETTERUTBETALING,
    FERIEPENGER,
    REVURDERING,
    UTBETALING,

    /**
     * This is a default enum value that will be used when attempting to deserialize unknown value.
     */
    @JsonEnumDefaultValue
    @Suppress("ktlint:standard:enum-entry-name-case")
    __UNKNOWN_VALUE,
}
