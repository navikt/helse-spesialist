package no.nav.helse.spesialist.api

class ApiModule {
    data class Configuration(
        val clientId: String,
        val issuerUrl: String,
        val jwkProviderUri: String,
        val tokenEndpoint: String,
    )
}
