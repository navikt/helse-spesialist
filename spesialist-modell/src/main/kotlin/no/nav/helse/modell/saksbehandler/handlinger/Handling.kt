package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.saksbehandler.Saksbehandler
import java.util.UUID

interface Handling {
    fun utførAv(saksbehandler: Saksbehandler)

    fun loggnavn(): String
}

interface Personhandling : Handling {
    val fødselsnummer: String
}

interface Overstyring : Personhandling {
    val saksbehandler: Saksbehandler
    val eksternHendelseId: UUID
}

abstract class Oppgavehandling(private val oppgaveId: Long) : Handling {
    protected lateinit var oppgave: Oppgave

    fun oppgaveId(): Long = oppgaveId

    fun oppgave(oppgave: Oppgave) {
        this.oppgave = oppgave
    }
}
