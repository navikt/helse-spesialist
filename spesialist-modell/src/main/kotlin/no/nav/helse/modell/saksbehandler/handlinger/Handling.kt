package no.nav.helse.modell.saksbehandler.handlinger

import java.time.LocalDate
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.saksbehandler.Saksbehandler

interface Handling {
    fun utførAv(saksbehandler: Saksbehandler)
    fun loggnavn(): String
}

interface Overstyring: Handling {
    fun gjelderFødselsnummer(): String
}

abstract class Oppgavehandling(private val oppgaveId: Long): Handling {
    protected lateinit var oppgave: Oppgave
    fun oppgaveId(): Long = oppgaveId
    fun oppgave(oppgave: Oppgave) {
        this.oppgave = oppgave
    }
}

interface PåVent: Handling {
    fun oppgaveId(): Long
    fun frist(): LocalDate?
    fun begrunnelse(): String?
}