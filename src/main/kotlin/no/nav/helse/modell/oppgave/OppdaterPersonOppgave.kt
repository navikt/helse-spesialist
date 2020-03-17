package no.nav.helse.modell.oppgave

import no.nav.helse.modell.SpleisBehov
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentNavnLøsning
import java.time.LocalDateTime

internal class OppdaterPersonOppgave(private val collector: SpleisBehov): Oppgave() {
    private val oppgaver: List<Oppgave> = listOf(
        HentNavnOppgave(),
        HentEnhetOppgave()
    )
    private inner class HentNavnOppgave: Oppgave() {
        override var ferdigstilt: LocalDateTime? = null
        override fun execute() {
            // Sjekk om har vært oppdatert i det siste
            // Spør DB
            // Hvis eksisterer - sjekk oppdatert i det siste
            // Hvis oppdatert i det siste: OK
            // Hvis ikke: HentNavn
            if (true) {
                collector.håndter(Behovtype.HentNavn)
            } else {
                ferdigstilt = LocalDateTime.now()
            }
        }

        override fun fortsett(løsning: HentEnhetLøsning) {
            // UPDATE TABLE SET enhetsnr = ? [...]
            ferdigstilt = LocalDateTime.now()
        }
    }
    private inner class HentEnhetOppgave : Oppgave() {
        override var ferdigstilt: LocalDateTime? = null
        override fun execute() {
            if ( true ) { // TODO: If Enhet ikke er oppdatert siste 5 dager
                collector.håndter(Behovtype.HentEnhet)
            } else {
                ferdigstilt = LocalDateTime.now()
            }
        }

        override fun fortsett(løsning: HentNavnLøsning) {
            // UPDATE TABLE SET fornavn=? [...]
            ferdigstilt = LocalDateTime.now()
        }
    }

    override val ferdigstilt: LocalDateTime? = null

    override fun execute() {
        oppgaver.execute()
    }
}
