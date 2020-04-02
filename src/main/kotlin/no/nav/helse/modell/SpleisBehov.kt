package no.nav.helse.modell

import no.nav.helse.modell.dao.*
import no.nav.helse.modell.dao.SpeilSnapshotRestDao
import no.nav.helse.modell.dao.VedtakDao
import no.nav.helse.modell.løsning.ArbeidsgiverLøsning
import no.nav.helse.modell.løsning.HentEnhetLøsning
import no.nav.helse.modell.løsning.HentPersoninfoLøsning
import no.nav.helse.modell.oppgave.*
import java.time.LocalDate
import java.util.*
import java.util.UUID.randomUUID

internal class SpleisBehov(
    internal val id: UUID,
    internal val fødselsnummer: String,
    internal val periodeFom: LocalDate,
    internal val periodeTom: LocalDate,
    internal val vedtaksperiodeId: UUID,
    internal val aktørId: String,
    internal val orgnummer: String,
    personDao: PersonDao,
    arbeidsgiverDao: ArbeidsgiverDao,
    vedtakDao: VedtakDao,
    snapshotDao: SnapshotDao,
    speilSnapshotRestDao: SpeilSnapshotRestDao,
    oppgaveDao: OppgaveDao
) {
    internal val uuid = randomUUID()
    internal val oppgaver: List<Oppgave> = listOf(
        OpprettPersonOppgave(this, personDao),
        OppdaterPersonOppgave(this, personDao),
        OpprettArbeidsgiverOppgave(this, arbeidsgiverDao),
        OppdatertArbeidsgiverOppgave(this, arbeidsgiverDao),
        OpprettVedtakOppgave(this, personDao, arbeidsgiverDao, vedtakDao, snapshotDao, speilSnapshotRestDao),
        OpprettOppgaveOppgave(this, oppgaveDao)
    )
    private val behovstyper: MutableList<Behovtype> = mutableListOf()

    internal fun execute() {
        oppgaver.executeAsSequence()
    }

    internal fun håndter(behovtype: Behovtype) {
        behovstyper.add(behovtype)
    }

    internal fun fortsett(løsning: HentEnhetLøsning) {
        oppgaver.current().fortsett(løsning)
    }

    internal fun fortsett(løsning: HentPersoninfoLøsning) {
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
