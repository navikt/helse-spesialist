package no.nav.helse.spesialist.application

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import java.time.Duration

interface Cache {
    fun <T> hentGjennomCache(
        key: String,
        type: TypeReference<T>,
        timeToLive: Duration,
        hentUtenomCache: () -> T,
    ): T
}

inline fun <reified T> Cache.hentGjennomCache(
    key: String,
    timeToLive: Duration,
    noinline hentUtenomCache: () -> T,
): T =
    hentGjennomCache(
        key = key,
        type = jacksonTypeRef<T>(),
        timeToLive = timeToLive,
        hentUtenomCache = hentUtenomCache,
    )
