package no.nav.helse.spesialist.valkey

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.valkey.DefaultJedisClientConfig
import io.valkey.HostAndPort
import io.valkey.JedisPooled
import no.nav.helse.spesialist.application.logg.loggDebug
import no.nav.helse.spesialist.application.logg.loggWarn
import java.time.Duration

class ValkeyCachingProxy(
    configuration: ValkeyModule.Configuration.Valkey,
) : CachingProxy {
    private val jedisPooled =
        JedisPooled(
            HostAndPort(configuration.host, configuration.port),
            DefaultJedisClientConfig
                .builder()
                .ssl(true)
                .user(configuration.username)
                .password(configuration.password)
                .build(),
        )

    private val objectMapper =
        jacksonObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .setDefaultPropertyInclusion(JsonInclude.Include.ALWAYS)

    private fun <T> get(
        key: String,
        type: TypeReference<T>,
    ): T? =
        runCatching { objectMapper.readValue(jedisPooled.get(key), type) }
            .getOrElse { throwable ->
                loggWarn("Feil ved henting / deserialisering av verdi fra Valkey", throwable, "key" to key)
                null
            }

    private fun <T> set(
        key: String,
        value: T,
        timeToLive: Duration,
    ) {
        runCatching { jedisPooled.setex(key, timeToLive.toSeconds(), objectMapper.writeValueAsString(value)) }
            .getOrElse { throwable ->
                loggWarn("Feil ved lagring / serialisering av verdi til Valkey", throwable, "key" to key)
            }
    }

    override fun <T : Any> get(
        key: String,
        type: TypeReference<T>,
        timeToLive: Duration,
        loadingFunction: () -> T?,
    ): T? =
        get(key, type).also { loggDebug("Valkey cache hit", "key" to key) }
            ?: run {
                loggDebug("Valkey cache miss", "key" to key)
                val value = loadingFunction()
                if (value != null) set(key, value, timeToLive)
                value
            }
}
