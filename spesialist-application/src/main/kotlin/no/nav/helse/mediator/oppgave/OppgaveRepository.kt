package no.nav.helse.mediator.oppgave

import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.saksbehandler.Tilgangskontroll

interface OppgaveRepository {
    fun lagre(oppgave: Oppgave)

    fun finn(
        id: Long,
        tilgangskontroll: Tilgangskontroll,
    ): Oppgave?
}
