package no.nav.helse.spesialist.api.rest

import kotlinx.serialization.Serializable
import no.nav.helse.spesialist.api.rest.saksbilde.ApiRisikovurdering

@Serializable
data class ApiRisikovurderingForVedtaksperiode(
    val risikovurdering: ApiRisikovurdering?,
)
