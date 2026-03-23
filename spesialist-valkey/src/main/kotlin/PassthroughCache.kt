package no.nav.helse.spesialist.valkey

import com.fasterxml.jackson.core.type.TypeReference
import no.nav.helse.spesialist.application.Cache
import no.nav.helse.spesialist.application.logg.loggWarn
import java.time.Duration

class PassthroughCache : Cache {
    init {
        loggWarn(
            "Bruker passthrough-variant av CachingProxy (i stedet for Valkey)." +
                " Det betyr at i praksis kjøres det uten cache.",
        )
    }

    override fun <T> hentGjennomCache(
        namespace: String,
        id: String,
        type: TypeReference<T>,
        timeToLive: Duration,
        hentUtenomCache: () -> T,
    ): T = hentUtenomCache()
}
