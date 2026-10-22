package no.nav.helse.spesialist.api

import io.ktor.utils.io.ConnectionClosedException
import tools.jackson.databind.DatabindException
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.CancellationException

internal fun DatabindException.erKlientFrakoblingUnderSerialisering(): Boolean {
    // Stopper ved første gjentakelse, så en sykel i årsakskjeden ikke gir evig løkke
    val besøkt = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
    return generateSequence<Throwable>(this) { it.cause }
        .takeWhile { besøkt.add(it) }
        .any { it.erKlientFrakobling() }
}

private fun Throwable.erKlientFrakobling(): Boolean {
    if (this is CancellationException || this is ConnectionClosedException) return true
    val melding = message ?: return false
    return melding.contains("Broken pipe", ignoreCase = true) ||
        melding.contains("Connection reset by peer", ignoreCase = true)
}
