package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.spesialist.domain.legacy.SaksbehandlerWrapper
import no.nav.helse.spesialist.domain.oppgave.Oppgave

interface Handling {
    fun utførAv(saksbehandlerWrapper: SaksbehandlerWrapper)

    fun loggnavn(): String
}

interface Personhandling : Handling {
    val fødselsnummer: String
}

abstract class Oppgavehandling(
    private val oppgaveId: Long,
) : Handling {
    protected lateinit var oppgave: Oppgave

    fun oppgaveId(): Long = oppgaveId

    fun oppgave(oppgave: Oppgave) {
        this.oppgave = oppgave
    }
}
