package no.nav.helse

import no.nav.helse.modell.VedtakDao
import java.util.*

internal class Automatiseringsdings(val vedtakDao: VedtakDao) {
    fun godkjentForAutomatisertBehandling(vedtaksperiodeId: UUID, eventId: UUID): Boolean {
        TODO("Not yet implemented")
    }
}
