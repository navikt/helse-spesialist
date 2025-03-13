package no.nav.helse.mediator.oppgave

import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.saksbehandler.Tilgangskontroll
import java.util.UUID

interface OppgaveRepository {
    fun lagre(oppgave: Oppgave)

    fun finn(
        id: Long,
        tilgangskontroll: Tilgangskontroll,
    ): Oppgave?

    /* TODO: Helst bør vi bruke finn(), men Oppgave er ikke et aggregat ennå,
        og metoden trenger derfor eksterne avhengigheter vi ikke vil ha (Tilgangskontroll) */
    fun finnSisteOppgaveTilstandForUtbetaling(utbetalingId: UUID): Oppgave.Tilstand?
}
