package no.nav.helse.spesialist.api.rest

import kotlinx.serialization.Serializable

@Serializable
data class ApiBruker(
    val brukerroller: Set<ApiBrukerrolle>,
    val tilganger: Set<ApiTilgang>,
)
