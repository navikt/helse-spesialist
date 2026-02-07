package no.nav.helse.spleis.graphql.hentsnapshot

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.annotation.JsonProperty

@Generated
public data class GraphQLArbeidsgiverrefusjon(
    @get:JsonProperty(value = "arbeidsgiver")
    public val arbeidsgiver: String,
    @get:JsonProperty(value = "refusjonsopplysninger")
    public val refusjonsopplysninger: List<GraphQLRefusjonselement>,
)
