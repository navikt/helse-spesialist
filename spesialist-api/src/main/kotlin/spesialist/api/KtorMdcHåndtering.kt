package no.nav.helse.spesialist.api

import io.ktor.server.application.ApplicationCall
import io.ktor.util.AttributeKey
import no.nav.helse.spesialist.application.logg.MdcKey
import no.nav.helse.spesialist.application.logg.coMedMdc
import no.nav.helse.spesialist.application.logg.medMdc

private val MDC_MAP_ATTRIBUTE = AttributeKey<MutableMap<MdcKey, String>>("mdc")

private val ApplicationCall.mutableMdcMapAttribute: MutableMap<MdcKey, String>
    get() = attributes.computeIfAbsent(MDC_MAP_ATTRIBUTE) { mutableMapOf() }

val ApplicationCall.mdcMapAttribute: Map<MdcKey, String>
    get() = mutableMdcMapAttribute

fun <T> ApplicationCall.medMdcOgAttribute(
    pair: Pair<MdcKey, String>,
    block: () -> T,
): T =
    medMdc(pair) {
        mutableMdcMapAttribute[pair.first] = pair.second
        block()
    }

suspend fun <T> ApplicationCall.coMedMdcOgAttribute(
    pair: Pair<MdcKey, String>,
    block: suspend () -> T,
): T =
    coMedMdc(pair) {
        mutableMdcMapAttribute[pair.first] = pair.second
        block()
    }
