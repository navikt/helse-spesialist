package no.nav.helse.spesialist.application.logg

import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

val teamLogs: Logger = LoggerFactory.getLogger("tjenestekall")
inline val <reified T> T.logg: Logger
    get() = LoggerFactory.getLogger(T::class.java)

inline fun <reified T> T.loggInfo(
    melding: String,
    sikkerloggDetaljer: String = "",
) {
    logg.info(melding)
    teamLogs.info(
        buildString {
            append(melding)
            if (sikkerloggDetaljer.isNotEmpty()) {
                append(" - ")
                append(sikkerloggDetaljer)
            }
        },
    )
}

inline fun <reified T> T.loggWarn(
    melding: String,
    sikkerloggDetaljer: String = "",
) {
    logg.warn(melding)
    teamLogs.warn(
        buildString {
            append(melding)
            if (sikkerloggDetaljer.isNotEmpty()) {
                append(" - ")
                append(sikkerloggDetaljer)
            }
        },
    )
}

inline fun <reified T> T.loggDebug(
    melding: String,
    sikkerloggDetaljer: String = "",
) {
    logg.debug(melding)
    teamLogs.debug(
        buildString {
            append(melding)
            if (sikkerloggDetaljer.isNotEmpty()) {
                append(" - ")
                append(sikkerloggDetaljer)
            }
        },
    )
}

inline fun <reified T> T.loggThrowable(
    message: String,
    teamLogsDetails: String = "",
    throwable: Throwable,
) {
    logg.error(message)
    teamLogs.error(
        buildString {
            append(message)
            if (teamLogsDetails.isNotEmpty()) {
                append(" - ")
                append(teamLogsDetails)
            }
        },
        throwable,
    )
}

inline fun <reified T> T.loggErrorWithNoThrowable(
    message: String,
    teamLogsDetails: String = "",
) {
    logg.error(message)
    teamLogs.error(
        buildString {
            append(message)
            if (teamLogsDetails.isNotEmpty()) {
                append(" - ")
                append(teamLogsDetails)
            }
        },
    )
}

inline fun <reified T> T.loggThrowable(
    message: String,
    throwable: Throwable,
) {
    loggThrowable(message, "", throwable)
}

inline fun <reified T> T.loggWarnThrowable(
    message: String,
    teamLogsDetails: String = "",
    throwable: Throwable,
) {
    logg.warn(message)
    teamLogs.warn(
        buildString {
            append(message)
            if (teamLogsDetails.isNotEmpty()) {
                append(" - ")
                append(teamLogsDetails)
            }
        },
        throwable,
    )
}

inline fun <reified T> T.loggWarnThrowable(
    message: String,
    throwable: Throwable,
) {
    loggWarnThrowable(message, "", throwable)
}

enum class MdcKey(
    val value: String,
) {
    BEHANDLING_UNIK_ID("behandlingUnikId"),
    CONTEXT_ID("contextId"),
    IDENTITETSNUMMER("identitetsnummer"),
    MELDING_ID("meldingId"),
    MELDINGNAVN("meldingnavn"),
    OPPRINNELIG_MELDING_ID("opprinneligMeldingId"),
    PERSON_PSEUDO_ID("personPseudoId"),
    REQUEST_METHOD("request.method"),
    REQUEST_URI("request.uri"),
    SAKSBEHANDLER_IDENT("saksbehandlerIdent"),
    SPLEIS_BEHANDLING_ID("spleisBehandlingId"),
    VEDTAKSPERIODE_ID("vedtaksperiodeId"),
}

fun <T> medMdc(
    vararg pairs: Pair<MdcKey, String>?,
    block: () -> T,
): T {
    val contextMap = MDC.getCopyOfContextMap() ?: emptyMap()
    try {
        MDC.setContextMap(contextMap + pairs.filterNotNull().map { it.first.value to it.second })
        return block()
    } finally {
        MDC.setContextMap(contextMap)
    }
}

suspend fun <T> coMedMdc(
    vararg pairs: Pair<MdcKey, String>?,
    block: suspend () -> T,
): T =
    withContext(
        MDCContext(
            MDC.getCopyOfContextMap().orEmpty() +
                pairs.filterNotNull().map { it.first.value to it.second },
        ),
    ) { block() }
