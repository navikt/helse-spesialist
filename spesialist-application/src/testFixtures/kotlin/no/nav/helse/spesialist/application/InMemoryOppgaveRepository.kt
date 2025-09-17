package no.nav.helse.spesialist.application

import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.modell.oppgave.Oppgave
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
}
