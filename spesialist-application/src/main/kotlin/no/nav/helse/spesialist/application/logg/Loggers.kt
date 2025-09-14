package no.nav.helse.spesialist.application.logg

import org.slf4j.Logger
import org.slf4j.LoggerFactory

val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
inline val <reified T> T.logg: Logger
    get() = LoggerFactory.getLogger(T::class.java)

inline fun <reified T> T.loggInfo(
    melding: String,
    sikkerloggDetaljer: String = "",
) {
    logg.info(melding)
    sikkerlogg.info(
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
    securelogDetails: String = "",
    throwable: Throwable,
) {
    logg.error(message)
    sikkerlogg.error(
        buildString {
            append(message)
            if (securelogDetails.isNotEmpty()) {
                append(" - ")
                append(securelogDetails)
            }
        },
        throwable,
    )
}

inline fun <reified T> T.loggThrowable(
    message: String,
    throwable: Throwable,
) {
    loggThrowable(message, "", throwable)
}
