package no.nav.helse.modell.oppgave

import java.time.LocalDateTime

abstract class Oppgave {
    internal abstract val ferdigstilt: LocalDateTime?
    internal abstract fun execute()
}

internal fun List<Oppgave>.execute() = this
    .filter { it.ferdigstilt == null }
    .forEach { it.execute() }
