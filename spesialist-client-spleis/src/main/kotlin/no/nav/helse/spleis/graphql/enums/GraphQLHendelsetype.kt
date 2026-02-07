package no.nav.helse.spleis.graphql.enums

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.`annotation`.JsonEnumDefaultValue
import com.fasterxml.jackson.`annotation`.JsonProperty

@Generated
public enum class GraphQLHendelsetype {
    @JsonProperty("InntektFraAOrdningen")
    INNTEKTFRAAORDNINGEN,

    @JsonProperty("Inntektsmelding")
    INNTEKTSMELDING,

    @JsonProperty("NySoknad")
    NYSOKNAD,

    @JsonProperty("SendtSoknadArbeidsgiver")
    SENDTSOKNADARBEIDSGIVER,

    @JsonProperty("SendtSoknadArbeidsledig")
    SENDTSOKNADARBEIDSLEDIG,

    @JsonProperty("SendtSoknadFrilans")
    SENDTSOKNADFRILANS,

    @JsonProperty("SendtSoknadNav")
    SENDTSOKNADNAV,

    @JsonProperty("SendtSoknadSelvstendig")
    SENDTSOKNADSELVSTENDIG,

    @JsonProperty("Ukjent")
    UKJENT,

    /**
     * This is a default enum value that will be used when attempting to deserialize unknown value.
     */
    @JsonEnumDefaultValue
    @Suppress("ktlint:standard:enum-entry-name-case")
    __UNKNOWN_VALUE,
}
