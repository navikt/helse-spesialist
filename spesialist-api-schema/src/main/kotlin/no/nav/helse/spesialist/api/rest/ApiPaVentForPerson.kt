package no.nav.helse.spesialist.api.rest

import kotlinx.serialization.Serializable
import no.nav.helse.spesialist.api.rest.saksbilde.ApiPaVent

@Serializable
data class ApiPaVentForPerson(
    val paVent: ApiPaVent?,
)
