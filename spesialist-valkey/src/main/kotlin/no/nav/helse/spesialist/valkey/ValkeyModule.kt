package no.nav.helse.spesialist.valkey

import no.nav.helse.spesialist.application.PersoninfoHenter

class ValkeyModule(
    configuration: Configuration,
    personinfoHenter: PersoninfoHenter,
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

    private val cachingProxy = configuration.valkey?.let(::ValkeyCachingProxy) ?: PassthroughCachingProxy()

    val cacheEnabledPersoninfoHenter =
        CacheEnabledPersoninfoHenter(
            personinfoHenter = personinfoHenter,
            cachingProxy = cachingProxy,
        )
}
