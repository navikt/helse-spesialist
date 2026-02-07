package no.nav.helse.spleis.graphql.enums

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.`annotation`.JsonEnumDefaultValue
import com.fasterxml.jackson.`annotation`.JsonProperty

@Generated
public enum class GraphQLPeriodetilstand {
    @JsonProperty("AnnulleringFeilet")
    ANNULLERINGFEILET,

    @JsonProperty("Annullert")
    ANNULLERT,

    @JsonProperty("AvventerAnnullering")
    AVVENTERANNULLERING,

    @JsonProperty("AvventerInntektsopplysninger")
    AVVENTERINNTEKTSOPPLYSNINGER,

    @JsonProperty("ForberederGodkjenning")
    FORBEREDERGODKJENNING,

    @JsonProperty("IngenUtbetaling")
    INGENUTBETALING,

    @JsonProperty("ManglerInformasjon")
    MANGLERINFORMASJON,

    @JsonProperty("RevurderingFeilet")
    REVURDERINGFEILET,

    @JsonProperty("TilAnnullering")
    TILANNULLERING,

    @JsonProperty("TilGodkjenning")
    TILGODKJENNING,

    @JsonProperty("TilInfotrygd")
    TILINFOTRYGD,

    @JsonProperty("TilSkjonnsfastsettelse")
    TILSKJONNSFASTSETTELSE,

    @JsonProperty("TilUtbetaling")
    TILUTBETALING,

    @JsonProperty("UtbetalingFeilet")
    UTBETALINGFEILET,

    @JsonProperty("Utbetalt")
    UTBETALT,

    @JsonProperty("UtbetaltVenterPaAnnenPeriode")
    UTBETALTVENTERPAANNENPERIODE,

    @JsonProperty("VenterPaAnnenPeriode")
    VENTERPAANNENPERIODE,

    /**
     * This is a default enum value that will be used when attempting to deserialize unknown value.
     */
    @JsonEnumDefaultValue
    __UNKNOWN_VALUE,
}
