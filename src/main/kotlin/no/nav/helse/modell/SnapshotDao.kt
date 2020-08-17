package no.nav.helse.modell

import kotliquery.sessionOf
import no.nav.helse.modell.vedtak.snapshot.oppdaterSnapshotForVedtaksperiode
import java.util.*
import javax.sql.DataSource

internal class SnapshotDao(private val dataSource: DataSource) {

    fun oppdaterSnapshotForVedtaksperiode(vedtaksperiodeId: UUID, snapshot: String) =
        sessionOf(dataSource).oppdaterSnapshotForVedtaksperiode(vedtaksperiodeId, snapshot)
}
