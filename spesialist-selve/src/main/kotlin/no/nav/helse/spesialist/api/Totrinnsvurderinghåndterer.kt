package no.nav.helse.spesialist.api

import java.util.UUID

interface Totrinnsvurderinghåndterer {
    fun erBeslutterOppgave(oppgaveId: Long): Boolean

    fun erEgenOppgave(
        oppgaveId: Long,
        saksbehandleroid: UUID?,
    ): Boolean
}
