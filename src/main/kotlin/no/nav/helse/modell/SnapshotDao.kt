package no.nav.helse.modell

import kotliquery.Session
import no.nav.helse.modell.vedtak.snapshot.oppdaterSnapshotForVedtaksperiode
import java.util.*

internal class SnapshotDao(private val session: Session) {

    fun oppdaterSnapshotForVedtaksperiode(vedtaksperiodeId: UUID, snapshot: String) =
        session.oppdaterSnapshotForVedtaksperiode(vedtaksperiodeId, snapshot)
}
