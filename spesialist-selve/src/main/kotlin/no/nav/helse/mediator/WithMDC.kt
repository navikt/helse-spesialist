package no.nav.helse.mediator

import org.slf4j.MDC

fun withMDC(
    context: Map<String, String>,
    block: () -> Unit,
) {
    val contextMap = MDC.getCopyOfContextMap() ?: emptyMap()
    try {
        MDC.setContextMap(contextMap + context)
        block()
    } finally {
        MDC.setContextMap(contextMap)
    }
}
