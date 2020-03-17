package no.nav.helse.modell

import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentNavnLøsning
import no.nav.helse.modell.oppgave.*
import no.nav.helse.modell.oppgave.OppdaterPersonOppgave
import no.nav.helse.modell.oppgave.execute
import java.util.UUID.*

internal class SpleisBehov(
    private val fødselsnummer: String,
    private val orgnummer: String
) {
    internal val uuid = randomUUID()
    internal val oppgaver: List<Oppgave> = listOf(OppdaterPersonOppgave(this))
    private val behovstyper: MutableList<Behovtype> = mutableListOf()

    internal fun start() {
        oppgaver.execute()
    }

    internal fun håndter(behovtype: Behovtype) {
        behovstyper.add(behovtype)
    }

    internal fun fortsett(behandlendeEnhet: HentEnhetLøsning) {

    }

    internal fun fortsett(hentNavnLøsning: HentNavnLøsning) {

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
