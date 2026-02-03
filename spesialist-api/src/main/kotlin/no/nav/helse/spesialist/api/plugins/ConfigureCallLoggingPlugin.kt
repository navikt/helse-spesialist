package no.nav.helse.spesialist.api.plugins

import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLoggingConfig
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.uri
import no.nav.helse.spesialist.api.mdcMapAttribute
import no.nav.helse.spesialist.application.logg.MdcKey
import no.nav.helse.spesialist.application.logg.teamLogs
import org.slf4j.event.Level

fun CallLoggingConfig.configureCallLoggingPlugin() {
    disableDefaultColors()
    logger = teamLogs
    level = Level.INFO
    callIdMdc("callId")
    mdc(MdcKey.REQUEST_METHOD.value) { it.request.httpMethod.value }
    mdc(MdcKey.REQUEST_URI.value) { it.request.uri }
    MdcKey.entries
        .filterNot { it in setOf(MdcKey.REQUEST_METHOD, MdcKey.REQUEST_URI) }
        .forEach { mdcKey ->
            mdc(mdcKey.value) { it.mdcMapAttribute[mdcKey] }
        }
    filter { call -> call.request.path().let { it.startsWith("/graphql") || it.startsWith("/api/") } }
}
