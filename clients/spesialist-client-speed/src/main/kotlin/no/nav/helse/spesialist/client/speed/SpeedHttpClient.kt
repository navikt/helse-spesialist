package no.nav.helse.spesialist.client.speed

import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.core5.http.HttpRequest
import org.apache.hc.core5.util.TimeValue

internal fun lagSpeedHttpClient(): CloseableHttpClient =
    HttpClients
        .custom()
        .setConnectionManager(
            PoolingHttpClientConnectionManagerBuilder
                .create()
                .setDefaultConnectionConfig(
                    ConnectionConfig
                        .custom()
                        .setValidateAfterInactivity(TimeValue.ofSeconds(2))
                        .build(),
                ).build(),
        ).evictIdleConnections(TimeValue.ofSeconds(30))
        .setRetryStrategy(SpeedRetryStrategy)
        .build()

// Kallene mot speed er POST, men leser bare data, så de er trygge å prøve på nytt
private object SpeedRetryStrategy : DefaultHttpRequestRetryStrategy(2, TimeValue.ofMilliseconds(100)) {
    override fun handleAsIdempotent(request: HttpRequest) = true
}
