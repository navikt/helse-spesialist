package no.nav.helse.spesialist.api.rest

import kotlinx.serialization.Serializable

@Serializable
data class ApiTildelingRequest(
    val navident: String,
)
