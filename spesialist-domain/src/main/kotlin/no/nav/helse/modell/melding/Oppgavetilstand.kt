package no.nav.helse.modell.melding

import no.nav.helse.modell.oppgave.Oppgave

data class OppgaveOpprettet(
    val oppgave: Oppgave,
) : UtgåendeHendelse

data class OppgaveOppdatert(
    val oppgave: Oppgave,
) : UtgåendeHendelse
