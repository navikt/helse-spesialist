package no.nav.helse.spesialist.api

import java.util.UUID
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType

interface Totrinnsvurderinghåndterer {
    fun settBeslutter(oppgaveId: Long, saksbehandlerOid: UUID)
    fun lagrePeriodehistorikk(
        oppgaveId: Long,
        saksbehandleroid: UUID?,
        type: PeriodehistorikkType,
        notat: Pair<String, NotatType>? = null
    )
    fun erBeslutterOppgave(oppgaveId: Long): Boolean
    fun erEgenOppgave(oppgaveId: Long, saksbehandleroid: UUID?): Boolean
}
