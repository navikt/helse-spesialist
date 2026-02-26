package no.nav.helse.spesialist.client.spiskammerset

import no.nav.helse.spesialist.application.logg.loggError
import no.nav.helse.spesialist.application.logg.loggWarn
import kotlin.math.pow

class ClientUtils {
    companion object {
        fun <T> retryMedBackoff(
            maxRetries: Int = 3,
            initialDelayMs: Long = 1000,
            block: () -> T,
        ): T {
            repeat(maxRetries) { attempt ->
                try {
                    return block()
                } catch (e: RetryableException) {
                    val delayMs = initialDelayMs * 3.0.pow(attempt.toDouble()).toLong()
                    loggWarn("Forsøk ${attempt + 1} av $maxRetries feilet: ${e.message}. Prøver igjen om ${delayMs}ms...")
                    if (attempt < maxRetries - 1) {
                        Thread.sleep(delayMs)
                    }
                }
            }
            loggError("Alle $maxRetries forsøk feilet")
            throw RuntimeException("Feil fra forsikringstjeneste: 500")
        }
    }
}

class RetryableException(
    message: String,
) : Exception(message)
