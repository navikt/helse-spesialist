package no.nav.helse.spesialist.application

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import java.time.Duration

interface Cache {
    fun <T> hentGjennomCache(
        namespace: String,
        id: String,
        type: TypeReference<T>,
        timeToLive: Duration,
        hentUtenomCache: () -> T,
    ): T
}

inline fun <reified T> Cache.hentGjennomCache(
    namespace: String,
    id: String,
    timeToLive: Duration,
    noinline hentUtenomCache: () -> T,
): T =
    hentGjennomCache(
        namespace = namespace,
        id = id,
        type = jacksonTypeRef<T>(),
        timeToLive = timeToLive,
        hentUtenomCache = hentUtenomCache,
    )
