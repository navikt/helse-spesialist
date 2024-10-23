package no.nav.helse.spesialist.api

import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import java.util.UUID

interface Totrinnsvurderinghåndterer {
    fun settBeslutter(
        oppgaveId: Long,
        saksbehandlerOid: UUID,
    )

    fun totrinnsvurderingRetur(
        oppgaveId: Long,
        saksbehandlerFraApi: SaksbehandlerFraApi,
        notat: String,
    )

    fun avventerTotrinnsvurdering(
        oppgaveId: Long,
        saksbehandlerFraApi: SaksbehandlerFraApi,
    )

    fun erBeslutterOppgave(oppgaveId: Long): Boolean

    fun erEgenOppgave(
        oppgaveId: Long,
        saksbehandleroid: UUID?,
    ): Boolean
}
