package no.nav.helse.modell

import no.nav.helse.modell.dao.ArbeidsgiverDao
import no.nav.helse.modell.dao.PersonDao
import no.nav.helse.modell.løsning.ArbeidsgiverLøsning
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentNavnLøsning
import no.nav.helse.modell.oppgave.*
import java.util.UUID.randomUUID

internal class SpleisBehov(
    internal val fødselsnummer: String,
    internal val aktørId: String,
    internal val orgnummer: String,
    personDao: PersonDao,
    arbeidsgiverDao: ArbeidsgiverDao
) {
    internal val uuid = randomUUID()
    internal val oppgaver: List<Oppgave> = listOf(
        OpprettPersonOppgave(this, personDao),
        OppdaterPersonOppgave(this, personDao),
        OpprettArbeidsgiverOppgave(this, arbeidsgiverDao),
        OppdatertArbeidsgiverOppgave(this, arbeidsgiverDao)
    )
    private val behovstyper: MutableList<Behovtype> = mutableListOf()

    internal fun start() {
        oppgaver.executeAsSequence()
    }

    internal fun håndter(behovtype: Behovtype) {
        behovstyper.add(behovtype)
    }

    internal fun fortsett(løsning: HentEnhetLøsning) {
        oppgaver.current().fortsett(løsning)
    }

    internal fun fortsett(løsning: HentNavnLøsning) {
        oppgaver.current().fortsett(løsning)
    }

    fun fortsett(løsning: ArbeidsgiverLøsning) {
        oppgaver.current().fortsett(løsning)
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
