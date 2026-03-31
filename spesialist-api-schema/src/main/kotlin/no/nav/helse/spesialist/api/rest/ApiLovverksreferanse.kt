package no.nav.helse.spesialist.api.rest

import kotlinx.serialization.Serializable

@Serializable
data class ApiLovverksreferanse(
    val paragraf: String,
    val ledd: String? = null,
    val bokstav: String? = null,
    val lovverk: String,
    val lovverksversjon: String,
)
