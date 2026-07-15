package no.nav.helse.spesialist.valkey

import com.fasterxml.jackson.annotation.JsonInclude
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import io.valkey.DefaultJedisClientConfig
import io.valkey.HostAndPort
import io.valkey.JedisPooled
import no.nav.helse.spesialist.application.Cache
import no.nav.helse.spesialist.application.logg.loggDebug
import no.nav.helse.spesialist.application.logg.loggWarn
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.time.Duration

class ValkeyCache(
    configuration: ValkeyModule.Configuration.Valkey,
) : Cache {
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
        jacksonMapperBuilder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .changeDefaultPropertyInclusion { incl -> incl.withValueInclusion(JsonInclude.Include.ALWAYS) }
            .changeDefaultPropertyInclusion { incl -> incl.withContentInclusion(JsonInclude.Include.ALWAYS) }
            .build()

    private fun <T> hentFraValkey(
        key: String,
        type: TypeReference<T>,
    ): T? =
        runCatching { jedisPooled.get(key)?.let { objectMapper.readValue(it, type) } }
            .getOrElse { throwable ->
                loggWarn("Feil ved henting / deserialisering av verdi fra Valkey", throwable, "key" to key)
                null
            }

    private fun <T> lagreTilValkey(
        key: String,
        value: T,
        timeToLive: Duration,
    ) {
        runCatching { jedisPooled.setex(key, timeToLive.toSeconds(), objectMapper.writeValueAsString(value)) }
            .getOrElse { throwable ->
                loggWarn("Feil ved lagring / serialisering av verdi til Valkey", throwable, "key" to key)
            }
    }

    override fun <T> hentGjennomCache(
        namespace: String,
        id: String,
        type: TypeReference<T>,
        timeToLive: Duration,
        hentUtenomCache: () -> T,
    ): T {
        val key = "$namespace:$id"
        return hentFraValkey(key, type)
            ?.also {
                loggDebug("Valkey cache hit for $namespace", "key" to key)
                hitCounter.withTag("namespace", namespace).increment()
            }
            ?: run {
                loggDebug("Valkey cache miss for $namespace", "key" to key)
                missCounter.withTag("namespace", namespace).increment()
                val value = hentUtenomCache()
                if (value != null) lagreTilValkey(key, value, timeToLive)
                value
            }
    }

    private val hitCounter =
        Counter
            .builder("spesialist.cache.valkey")
            .tag("result", "hit")
            .withRegistry(Metrics.globalRegistry)

    private val missCounter =
        Counter
            .builder("spesialist.cache.valkey")
            .tag("result", "miss")
            .withRegistry(Metrics.globalRegistry)
}
