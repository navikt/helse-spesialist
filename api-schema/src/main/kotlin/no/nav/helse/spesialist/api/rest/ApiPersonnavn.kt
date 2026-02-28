package no.nav.helse.spesialist.api.rest

import kotlinx.serialization.Serializable

@Serializable
data class ApiPersonnavn(
    val fornavn: String,
    val etternavn: String,
    val mellomnavn: String?,
)
