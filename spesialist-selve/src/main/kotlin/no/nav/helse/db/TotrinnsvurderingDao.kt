package no.nav.helse.db

import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingOld
import java.util.UUID

interface TotrinnsvurderingDao {
    fun hentAktivTotrinnsvurdering(oppgaveId: Long): Pair<Long, TotrinnsvurderingFraDatabase>?

    fun oppdater(totrinnsvurderingFraDatabase: TotrinnsvurderingFraDatabase)

    fun settBeslutter(
        oppgaveId: Long,
        saksbehandlerOid: UUID,
    )

    fun settErRetur(vedtaksperiodeId: UUID)

    fun opprettOld(vedtaksperiodeId: UUID): TotrinnsvurderingOld

    fun hentAktiv(oppgaveId: Long): TotrinnsvurderingOld?

    fun hentAktiv(vedtaksperiodeId: UUID): TotrinnsvurderingOld?

    fun ferdigstill(vedtaksperiodeId: UUID)
}
