package no.nav.helse.modell.oppgave

import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentNavnLøsning
import java.time.LocalDateTime

abstract class Oppgave {
    internal abstract val ferdigstilt: LocalDateTime?
    internal abstract fun execute()
    internal open fun fortsett(løsning: HentEnhetLøsning) {}
    internal open fun fortsett(løsning: HentNavnLøsning) {}
}

internal fun List<Oppgave>.execute() = this
    .filter { it.ferdigstilt == null }
    .forEach { it.execute() }

internal fun List<Oppgave>.executeAsSequence() = this
    .filter { it.ferdigstilt == null }
    .onEach { it.execute() }
    .takeWhile { it.ferdigstilt != null }
