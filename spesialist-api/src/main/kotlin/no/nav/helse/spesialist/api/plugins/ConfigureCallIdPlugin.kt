package no.nav.helse.spesialist.api.plugins

import io.ktor.http.HttpHeaders
import io.ktor.server.plugins.callid.CallIdConfig
import java.util.UUID

fun CallIdConfig.configureCallIdPlugin() {
    retrieveFromHeader(HttpHeaders.XRequestId)
    generate {
        UUID.randomUUID().toString()
    }
}
