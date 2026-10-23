package no.nav.helse.spesialist.application.logg

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply

/**
 * Ktor logger kansellerte kall som ERROR («Unhandled server error») når serveren stenger ned
 * og svaret ikke kan sendes. Det er forventet ved nedstenging, og Ktor logger selv
 * kansellering på DEBUG andre steder, så disse hendelsene droppes.
 */
class SkipKansellertVedNedstengingFilter : Filter<ILoggingEvent>() {
    override fun decide(event: ILoggingEvent): FilterReply =
        if (event.level == Level.ERROR &&
            event.loggerName == KTOR_APPLICATION_LOGGER &&
            event.throwableProxy?.className in KANSELLERINGSTYPER
        ) {
            FilterReply.DENY
        } else {
            FilterReply.NEUTRAL
        }

    private companion object {
        const val KTOR_APPLICATION_LOGGER = "no.nav.helse.rapids_rivers.ktorApplication"
        val KANSELLERINGSTYPER =
            setOf(
                "kotlinx.coroutines.JobCancellationException",
                "java.util.concurrent.CancellationException",
            )
    }
}
