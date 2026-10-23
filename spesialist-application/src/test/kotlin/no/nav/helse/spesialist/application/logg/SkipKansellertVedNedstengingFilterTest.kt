package no.nav.helse.spesialist.application.logg

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.spi.FilterReply
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.CancellationException

class SkipKansellertVedNedstengingFilterTest {
    private val filter = SkipKansellertVedNedstengingFilter()

    @Test
    fun `dropper ERROR med JobCancellationException fra Ktor-loggeren`() {
        assertEquals(FilterReply.DENY, filter.decide(event(throwable = jobCancellationException())))
    }

    @Test
    fun `dropper ERROR med CancellationException fra Ktor-loggeren`() {
        assertEquals(FilterReply.DENY, filter.decide(event(throwable = CancellationException("kansellert"))))
    }

    @Test
    fun `slipper gjennom andre exceptions fra Ktor-loggeren`() {
        assertEquals(FilterReply.NEUTRAL, filter.decide(event(throwable = IllegalStateException("feil"))))
    }

    @Test
    fun `slipper gjennom kansellering fra andre loggere`() {
        assertEquals(
            FilterReply.NEUTRAL,
            filter.decide(event(loggerName = "no.nav.helse.annen", throwable = jobCancellationException())),
        )
    }

    @Test
    fun `slipper gjennom kansellering på andre nivåer`() {
        assertEquals(FilterReply.NEUTRAL, filter.decide(event(level = Level.WARN, throwable = jobCancellationException())))
    }

    @Test
    fun `slipper gjennom ERROR uten exception`() {
        assertEquals(FilterReply.NEUTRAL, filter.decide(event(throwable = null)))
    }

    private fun event(
        loggerName: String = "no.nav.helse.rapids_rivers.ktorApplication",
        level: Level = Level.ERROR,
        throwable: Throwable?,
    ) = LoggingEvent(
        "fqcn",
        LoggerContext().getLogger(loggerName),
        level,
        "Unhandled server error",
        throwable,
        null,
    )

    private fun jobCancellationException(): Throwable =
        runBlocking {
            runCatching { CompletableDeferred<Unit>().apply { cancel() }.await() }.exceptionOrNull()!!
        }
}
