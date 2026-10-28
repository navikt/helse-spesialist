package no.nav.helse.spesialist.api.rest

import kotlinx.serialization.Serializable

@Serializable
data class ApiTildelingForPerson(
    val tildeling: ApiTildeling?,
)
