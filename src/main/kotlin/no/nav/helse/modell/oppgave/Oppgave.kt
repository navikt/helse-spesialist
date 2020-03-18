package no.nav.helse.modell.oppgave

import no.nav.helse.modell.løsning.ArbeidsgiverLøsning
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentPersoninfoLøsning
import java.time.LocalDateTime

abstract class Oppgave {
    internal open val oppgaver: List<Oppgave> = listOf()
    internal abstract var ferdigstilt: LocalDateTime?
    internal abstract fun execute()
    internal open fun fortsett(hentEnhetLøsning: HentEnhetLøsning) {
        oppgaver.forEach { it.fortsett(hentEnhetLøsning) }
    }
    internal open fun fortsett(hentPersoninfoLøsning: HentPersoninfoLøsning) {
        oppgaver.forEach { it.fortsett(hentPersoninfoLøsning) }
    }

    internal open fun fortsett(løsning: ArbeidsgiverLøsning) {
        oppgaver.forEach { it.fortsett(løsning) }
    }
}

internal fun List<Oppgave>.execute() = this
    .filter { it.ferdigstilt == null }
    .forEach { it.execute() }

internal fun List<Oppgave>.executeAsSequence() = this
    .asSequence()
    .filter { it.ferdigstilt == null }
    .onEach { it.execute() }
    .takeWhile { it.ferdigstilt != null }
    .forEach { println("Ferdigstilt oppgave") } // TODO

internal fun List<Oppgave>.current() = asSequence()
    .first { it.ferdigstilt == null }
