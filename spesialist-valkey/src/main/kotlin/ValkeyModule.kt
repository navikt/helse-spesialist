package no.nav.helse.spesialist.valkey

import no.nav.helse.spesialist.application.Cache

class ValkeyModule(
    configuration: Configuration,
) {
    data class Configuration(
        val valkey: Valkey?,
    ) {
        data class Valkey(
            val host: String,
            val port: Int,
            val username: String,
            val password: String,
        )
    }

    val cache: Cache = configuration.valkey?.let(::ValkeyCache) ?: PassthroughCache()
}
