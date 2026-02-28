package no.nav.helse.spesialist.valkey

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import java.time.Duration

interface CachingProxy {
    fun <T : Any> get(
        key: String,
        type: TypeReference<T>,
        timeToLive: Duration,
        loadingFunction: () -> T?,
    ): T?
}

inline fun <reified T : Any> CachingProxy.get(
    key: String,
    timeToLive: Duration,
    noinline loadFunction: () -> T?,
): T? =
    get(
        key = key,
        type = jacksonTypeRef<T>(),
        timeToLive = timeToLive,
        loadingFunction = loadFunction,
    )
