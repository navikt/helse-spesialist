package no.nav.helse.modell.oppgave

import no.nav.helse.modell.SpleisBehov
import java.time.LocalDateTime

internal class OpprettPersonOppgave(private val collector: SpleisBehov): Oppgave() {
    override var ferdigstilt: LocalDateTime? = null

    override fun execute() {
        if (true) { // Person eksisterer
            ferdigstilt = LocalDateTime.now()
        } else {

        }
    }

}
