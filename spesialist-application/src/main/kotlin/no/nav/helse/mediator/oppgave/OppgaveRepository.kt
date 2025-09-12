package no.nav.helse.mediator.oppgave

import no.nav.helse.modell.oppgave.Oppgave
import java.util.UUID

interface OppgaveRepository {
    fun lagre(oppgave: Oppgave)

    fun finn(id: Long): Oppgave?

    // TODO: Helst bør vi bruke finn(), men Oppgave er ikke et aggregat ennå
    fun finnSisteOppgaveForUtbetaling(utbetalingId: UUID): OppgaveTilstandStatusOgGodkjenningsbehov?

    data class OppgaveTilstandStatusOgGodkjenningsbehov(
        val id: Long,
        val tilstand: Oppgave.Tilstand,
        val godkjenningsbehovId: UUID,
        val utbetalingId: UUID,
    )
}
