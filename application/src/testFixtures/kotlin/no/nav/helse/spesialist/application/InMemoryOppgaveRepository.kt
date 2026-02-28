package no.nav.helse.spesialist.application

import no.nav.helse.db.PersonnavnFraDatabase
import no.nav.helse.db.SorteringsnøkkelForDatabase
import no.nav.helse.db.Sorteringsrekkefølge
import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.mediator.oppgave.OppgaveRepository.BehandletOppgaveProjeksjon
import no.nav.helse.mediator.oppgave.OppgaveRepository.OppgaveProjeksjon
import no.nav.helse.mediator.oppgave.OppgaveRepository.Side
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class InMemoryOppgaveRepository : OppgaveRepository {
    private val oppgaver = mutableMapOf<Long, Oppgave>()

    fun alle(): List<Oppgave> = oppgaver.values.toList()

    override fun lagre(oppgave: Oppgave) {
        oppgaver[oppgave.id] = oppgave
    }

    override fun finn(id: Long): Oppgave? = oppgaver[id]

    override fun finn(id: SpleisBehandlingId): Oppgave? = oppgaver.values.find { it.behandlingId == id.value }

    override fun finnAktivForPerson(identitetsnummer: Identitetsnummer): Oppgave? {
        error("Not implemented for this test")
    }

    override fun finnSisteOppgaveForUtbetaling(utbetalingId: UUID): OppgaveRepository.OppgaveTilstandStatusOgGodkjenningsbehov? =
        oppgaver.values.firstOrNull { it.utbetalingId == utbetalingId }?.let {
            OppgaveRepository.OppgaveTilstandStatusOgGodkjenningsbehov(
                id = it.id,
                tilstand = it.tilstand,
                godkjenningsbehovId = it.godkjenningsbehovId,
                utbetalingId = it.utbetalingId,
            )
        }

    override fun førsteOpprettetForBehandlingId(behandlingId: UUID): LocalDateTime? = oppgaver.values.filter { it.behandlingId == behandlingId }.minOfOrNull { it.opprettet }

    override fun finnOppgaveProjeksjoner(
        minstEnAvEgenskapene: List<Set<Egenskap>>,
        ingenAvEgenskapene: Set<Egenskap>,
        erTildelt: Boolean?,
        tildeltTilOid: SaksbehandlerOid?,
        erPåVent: Boolean?,
        ikkeSendtTilBeslutterAvOid: SaksbehandlerOid?,
        sorterPå: SorteringsnøkkelForDatabase,
        sorteringsrekkefølge: Sorteringsrekkefølge,
        sidetall: Int,
        sidestørrelse: Int,
    ): Side<OppgaveProjeksjon> {
        error("Not implemented for this test")
    }

    override fun finnBehandledeOppgaveProjeksjoner(
        fom: LocalDate,
        tom: LocalDate,
        sidetall: Int,
        sidestørrelse: Int,
        behandletAvOid: UUID
    ) = Side(
        totaltAntall = oppgaver.keys.first(),
        sidetall = sidetall,
        sidestørrelse = sidestørrelse,
        elementer = oppgaver.values.map { it.toBehandletOppgaveProjeksjon() }
    )
}

private fun Oppgave.toBehandletOppgaveProjeksjon(): BehandletOppgaveProjeksjon {
    // Denne skal egentlig hente data fra oppgave, men det blir for knotete siden oppgave ikke er skrevet om til DDD ennå.
    return BehandletOppgaveProjeksjon(
        id = id,
        fødselsnummer = "42",
        ferdigstiltTidspunkt = LocalDateTime.of(2020, 2, 2, 12, 30),
        saksbehandler = "her skal saksbehandlers navn stå",
        beslutter = "her skal beslutters navn eventuelt stå",
        personnavn = PersonnavnFraDatabase(
            fornavn = "navnet",
            mellomnavn = "til",
            etternavn = "personen",
        ),
    )
}
