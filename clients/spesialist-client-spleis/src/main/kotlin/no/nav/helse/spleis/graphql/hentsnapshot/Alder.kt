package no.nav.helse.spleis.graphql.hentsnapshot

import com.expediagroup.graphql.client.Generated
import com.fasterxml.jackson.annotation.JsonProperty

@Generated
public data class Alder(
    @get:JsonProperty(value = "alderSisteSykedag")
    public val alderSisteSykedag: Int,
    @get:JsonProperty(value = "oppfylt")
    public val oppfylt: Boolean,
)
