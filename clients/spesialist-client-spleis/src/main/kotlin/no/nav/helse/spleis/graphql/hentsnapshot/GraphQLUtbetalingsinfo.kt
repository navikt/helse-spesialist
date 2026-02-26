package no.nav.helse.spleis.graphql.hentsnapshot

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.annotation.JsonProperty

@Generated
public data class GraphQLUtbetalingsinfo(
    @get:JsonProperty(value = "arbeidsgiverbelop")
    public val arbeidsgiverbelop: Int? = null,
    @get:JsonProperty(value = "inntekt")
    public val inntekt: Int? = null,
    @get:JsonProperty(value = "personbelop")
    public val personbelop: Int? = null,
    @get:JsonProperty(value = "refusjonsbelop")
    public val refusjonsbelop: Int? = null,
    @get:JsonProperty(value = "totalGrad")
    public val totalGrad: Double? = null,
    @get:JsonProperty(value = "utbetaling")
    public val utbetaling: Int? = null,
)
