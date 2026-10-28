package no.nav.helse.spesialist.api.rest

import kotlinx.serialization.Serializable
import no.nav.helse.spesialist.api.rest.saksbilde.ApiTotrinnsvurdering

@Serializable
data class ApiTotrinnsvurderingForPerson(
    val totrinnsvurdering: ApiTotrinnsvurdering?,
)
