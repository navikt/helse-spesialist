package no.nav.helse.modell.oppgave

import no.nav.helse.modell.SpleisBehov
import java.time.LocalDateTime

internal class OppdaterPersonOppgave(private val collector: SpleisBehov): Oppgave() {

    private val oppgaver: List<Oppgave> = listOf(HentNavnOppgave(), HentEnhetOppgave())

    private inner class HentNavnOppgave: Oppgave() {
        override var ferdigstilt: LocalDateTime? = null
        override fun execute() {
            // Sjekk om eksisteerer eller har vært oppdatert i det siste
            // Spør DB
            // Hvis eksisterer - sjekk oppdatert i det siste
            // Hvis oppdatert i det siste: OK
            // Hvis ikke: HentNavn
        }
    }
    private inner class HentEnhetOppgave : Oppgave() {
        override var ferdigstilt: LocalDateTime? = null
        override fun execute() {
            if ( true ) // TODO: If Enhet ikke er oppdatert siste 5 dager
            collector.håndter(Behovtype.HENT_ENHET)
        }
    }

    override val ferdigstilt: LocalDateTime? = null

    override fun execute() {
        oppgaver.execute()
    }
}
