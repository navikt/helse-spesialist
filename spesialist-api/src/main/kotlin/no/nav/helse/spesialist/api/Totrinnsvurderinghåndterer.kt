package no.nav.helse.spesialist.api

import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import java.util.UUID

interface Totrinnsvurderinghåndterer {
    fun settBeslutter(
        oppgaveId: Long,
        saksbehandlerOid: UUID,
    )

    fun lagrePeriodehistorikk(
        oppgaveId: Long,
        saksbehandleroid: UUID?,
        type: PeriodehistorikkType,
        notat: Pair<String, NotatType>? = null,
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
