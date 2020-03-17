package no.nav.helse.modell.oppgave

import no.nav.helse.modell.SpleisBehov
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentNavnLøsning
import java.time.LocalDateTime

internal class OpprettPersonOppgave(private val collector: SpleisBehov) : Oppgave() {
    override var ferdigstilt: LocalDateTime? = null
    private var navnId: Int? = null
    private var enhetId: Int? = null

    override fun execute() {
        if (true) { // Person eksisterer
            ferdigstilt = LocalDateTime.now()
        } else if (navnId != null && enhetId != null) {
            // INSERT PERSON
            ferdigstilt = LocalDateTime.now()
        }
        else {
            collector.håndter(Behovtype.HentNavn)
            collector.håndter(Behovtype.HentEnhet)
        }
    }

    override fun fortsett(hentEnhetLøsning: HentEnhetLøsning) {
        enhetId = hentEnhetLøsning.enhetNr.toInt()
    }

    override fun fortsett(hentNavnLøsning: HentNavnLøsning) {
        // navnId = INSERT NAVN
    }

}
