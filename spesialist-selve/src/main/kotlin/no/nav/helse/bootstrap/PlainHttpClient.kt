package no.nav.helse.bootstrap

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter

internal fun httpClient(
    socketTimeout: Int,
    connectTimeout: Int,
    connectionRequestTimeout: Int,
) = HttpClient(Apache) {
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter())
    }
    engine {
        this.socketTimeout = socketTimeout
        this.connectTimeout = connectTimeout
        this.connectionRequestTimeout = connectionRequestTimeout
    }
}
