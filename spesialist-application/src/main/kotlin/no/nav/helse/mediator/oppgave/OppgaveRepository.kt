package no.nav.helse.mediator.oppgave

import no.nav.helse.modell.oppgave.Oppgave
import java.time.LocalDateTime
import java.util.UUID

interface OppgaveRepository {
    fun lagre(oppgave: Oppgave)

    fun finn(id: Long): Oppgave?

    // TODO: Helst bør vi bruke finn(), men Oppgave er ikke et aggregat ennå
    fun finnSisteOppgaveForUtbetaling(utbetalingId: UUID): OppgaveTilstandStatusOgGodkjenningsbehov?

    fun førsteOpprettetForBehandlingId(behandlingId: UUID): LocalDateTime?

    fun finnFlereAvventerSaksbehandlerUtenFørsteOpprettet(antall: Int): List<Oppgave>

    data class OppgaveTilstandStatusOgGodkjenningsbehov(
        val id: Long,
        val tilstand: Oppgave.Tilstand,
        val godkjenningsbehovId: UUID,
        val utbetalingId: UUID,
    )
}
