package no.nav.helse.spleis.graphql.enums

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.`annotation`.JsonEnumDefaultValue
import com.fasterxml.jackson.`annotation`.JsonProperty

@Generated
public enum class GraphQLSykdomsdagtype {
    @JsonProperty("AndreYtelserAap")
    ANDREYTELSERAAP,

    @JsonProperty("AndreYtelserDagpenger")
    ANDREYTELSERDAGPENGER,

    @JsonProperty("AndreYtelserForeldrepenger")
    ANDREYTELSERFORELDREPENGER,

    @JsonProperty("AndreYtelserOmsorgspenger")
    ANDREYTELSEROMSORGSPENGER,

    @JsonProperty("AndreYtelserOpplaringspenger")
    ANDREYTELSEROPPLARINGSPENGER,

    @JsonProperty("AndreYtelserPleiepenger")
    ANDREYTELSERPLEIEPENGER,

    @JsonProperty("AndreYtelserSvangerskapspenger")
    ANDREYTELSERSVANGERSKAPSPENGER,

    @JsonProperty("ArbeidIkkeGjenopptattDag")
    ARBEIDIKKEGJENOPPTATTDAG,

    @JsonProperty("Arbeidsdag")
    ARBEIDSDAG,

    @JsonProperty("Arbeidsgiverdag")
    ARBEIDSGIVERDAG,

    @JsonProperty("Avslatt")
    AVSLATT,

    @JsonProperty("Feriedag")
    FERIEDAG,

    @JsonProperty("ForeldetSykedag")
    FORELDETSYKEDAG,

    @JsonProperty("FriskHelgedag")
    FRISKHELGEDAG,

    @JsonProperty("MeldingTilNavDag")
    MELDINGTILNAVDAG,

    @JsonProperty("Permisjonsdag")
    PERMISJONSDAG,

    @JsonProperty("SykHelgedag")
    SYKHELGEDAG,

    @JsonProperty("Sykedag")
    SYKEDAG,

    @JsonProperty("SykedagNav")
    SYKEDAGNAV,

    @JsonProperty("Ubestemtdag")
    UBESTEMTDAG,

    /**
     * This is a default enum value that will be used when attempting to deserialize unknown value.
     */
    @JsonEnumDefaultValue
    __UNKNOWN_VALUE,
}
