package no.nav.helse.modell.totrinnsvurdering

import no.nav.helse.db.TotrinnsvurderingDao
import java.util.UUID

class TotrinnsvurderingService(
    private val totrinnsvurderingDao: TotrinnsvurderingDao,
) {
    fun hentAktiv(vedtaksperiodeId: UUID): TotrinnsvurderingOld? = totrinnsvurderingDao.hentAktiv(vedtaksperiodeId)
}
