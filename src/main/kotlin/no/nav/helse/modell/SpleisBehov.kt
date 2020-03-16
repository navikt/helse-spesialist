package no.nav.helse.modell

import no.nav.helse.modell.oppgave.*
import no.nav.helse.modell.oppgave.OppdaterPersonOppgave
import no.nav.helse.modell.oppgave.execute
import java.util.UUID.*

internal class SpleisBehov(private val fødselsnummer: String, private val orgnummer: String) {
    internal val uuid = randomUUID()
    internal val oppgaver: List<Oppgave> = listOf(OppdaterPersonOppgave(this))
    internal val behov: MutableList<Behov> = mutableListOf()

    internal fun start() {
        oppgaver.execute()
    }

    internal fun håndter(behovtype: Behovtype) {
        behov.add(Behov(behovtype, fødselsnummer, uuid))
    }
}
