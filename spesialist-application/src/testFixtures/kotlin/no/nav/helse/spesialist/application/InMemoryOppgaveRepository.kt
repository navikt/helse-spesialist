package no.nav.helse.spesialist.application

import no.nav.helse.db.SorteringsnøkkelForDatabase
import no.nav.helse.db.Sorteringsrekkefølge
import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.mediator.oppgave.OppgaveRepository.OppgaveProjeksjon
import no.nav.helse.mediator.oppgave.OppgaveRepository.Side
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import java.time.LocalDateTime
import java.util.UUID

class InMemoryOppgaveRepository : OppgaveRepository {
    private val oppgaver = mutableMapOf<Long, Oppgave>()

    fun alle(): List<Oppgave> =
        oppgaver.values.toList()

    override fun lagre(oppgave: Oppgave) {
        oppgaver[oppgave.id] = oppgave
    }

    override fun finn(id: Long): Oppgave? =
        oppgaver[id]

    override fun finnSisteOppgaveForUtbetaling(utbetalingId: UUID): OppgaveRepository.OppgaveTilstandStatusOgGodkjenningsbehov? {
        return oppgaver.values.firstOrNull { it.utbetalingId == utbetalingId }?.let {
            OppgaveRepository.OppgaveTilstandStatusOgGodkjenningsbehov(
                id = it.id,
                tilstand = it.tilstand,
                godkjenningsbehovId = it.godkjenningsbehovId,
                utbetalingId = it.utbetalingId,
            )
        }
    }

    override fun førsteOpprettetForBehandlingId(behandlingId: UUID): LocalDateTime? =
        oppgaver.values.filter { it.behandlingId == behandlingId }.minOfOrNull { it.opprettet }

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
        sidestørrelse: Int
    ): Side<OppgaveProjeksjon> {
        error("Not implemented for this test")
    }
}
