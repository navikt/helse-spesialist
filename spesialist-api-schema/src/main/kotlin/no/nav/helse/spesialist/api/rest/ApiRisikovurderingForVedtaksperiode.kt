package no.nav.helse.spesialist.api.rest

import kotlinx.serialization.Serializable

@Serializable
data class ApiRisikovurderingForVedtaksperiode(
    val risikovurdering: ApiRisikovurdering?,
)

@Serializable
data class ApiRisikovurdering(
    val funn: List<ApiFaresignal>?,
    val kontrollertOk: List<ApiFaresignal>,
)

@Serializable
data class ApiFaresignal(
    val beskrivelse: String,
    val kategori: List<String>,
)
