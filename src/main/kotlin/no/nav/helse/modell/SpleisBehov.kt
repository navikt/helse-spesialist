package no.nav.helse.modell

import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentNavnLøsning
import no.nav.helse.modell.oppgave.*
import java.util.UUID.randomUUID

internal class SpleisBehov(
    private val fødselsnummer: String,
    private val orgnummer: String
) {
    internal val uuid = randomUUID()
    internal val oppgaver: List<Oppgave> = listOf(
        OpprettPersonOppgave(this),
        OppdaterPersonOppgave(this)
    )
    private val behovstyper: MutableList<Behovtype> = mutableListOf()

    internal fun start() {
        oppgaver.executeAsSequence()
    }

    internal fun håndter(behovtype: Behovtype) {
        behovstyper.add(behovtype)
    }

    internal fun fortsett(behandlendeEnhet: HentEnhetLøsning) {
        oppgaver.forEach { it.fortsett(behandlendeEnhet) }
        start()
    }

    internal fun fortsett(hentNavnLøsning: HentNavnLøsning) {
        oppgaver.forEach { it.fortsett(hentNavnLøsning) }
        start()
    }


    fun behov() = behovstyper.takeIf { it.isNotEmpty() }?.let { typer ->
        Behov(
            typer = typer,
            fødselsnummer = fødselsnummer,
            orgnummer = orgnummer,
            spleisBehovId = uuid
        )
    }
}
