package no.nav.helse.spesialist.application.logg

import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
