package no.nav.helse.bootstrap

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
import org.slf4j.LoggerFactory
import java.net.ProxySelector

private val logg = LoggerFactory.getLogger("AzureAdClient")

internal fun azureAdClient() =
    HttpClient(Apache) {
        install(HttpRequestRetry) {
            retryOnExceptionIf(3) { request, throwable ->
                logg.warn("Caught exception ${throwable.message}, for url ${request.url}")
                true
            }
            retryIf(maxRetries) { request, response ->
                if (response.status.value.let { it in 500..599 }) {
                    logg.warn(
                        "Retrying for statuscode ${response.status.value}, for url ${request.url}",
                    )
                    true
                } else {
                    false
                }
            }
        }
        engine {
            customizeClient {
                setRoutePlanner(SystemDefaultRoutePlanner(ProxySelector.getDefault()))
            }
        }
        install(ContentNegotiation) {
            register(
                ContentType.Application.Json,
                JacksonConverter(
                    jacksonObjectMapper()
                        .registerModule(JavaTimeModule()),
                ),
            )
        }
    }
