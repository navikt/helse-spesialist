package no.nav.helse.modell.oppgave

import no.nav.helse.modell.SpleisBehov
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentNavnLøsning
import java.time.LocalDateTime

internal class OppdaterPersonOppgave(private val collector: SpleisBehov): Oppgave() {
    override val ferdigstilt: LocalDateTime? = null
    private val oppgaver: List<Oppgave> = listOf(
        HentNavnOppgave(),
        HentEnhetOppgave()
    )

    private inner class HentNavnOppgave: Oppgave() {
        override var ferdigstilt: LocalDateTime? = null

        override fun execute() {
            if (true) { // TODO: If Navn ikke er oppdatert siste 14 dager
                collector.håndter(Behovtype.HentNavn)
            } else {
                ferdigstilt = LocalDateTime.now()
            }
        }

        override fun fortsett(løsning: HentNavnLøsning) {
            // UPDATE TABLE SET fornavn=? [...]
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

        override fun fortsett(løsning: HentEnhetLøsning) {
            // UPDATE TABLE SET enhetsnr = ? [...]
            ferdigstilt = LocalDateTime.now()
        }
    }


    override fun execute() {
        oppgaver.execute()
    }
}
