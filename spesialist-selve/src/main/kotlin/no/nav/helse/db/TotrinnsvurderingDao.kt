package no.nav.helse.db

import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingOld
import java.util.UUID

interface TotrinnsvurderingDao {
    fun hentAktivTotrinnsvurdering(oppgaveId: Long): Pair<Long, TotrinnsvurderingFraDatabase>?

    fun oppdater(totrinnsvurderingFraDatabase: TotrinnsvurderingFraDatabase)

    fun hentAktiv(vedtaksperiodeId: UUID): TotrinnsvurderingOld?
}
