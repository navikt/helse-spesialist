package no.nav.helse.modell

import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.vedtak.snapshot.insertSpeilSnapshot
import no.nav.helse.modell.vedtak.snapshot.oppdaterSnapshotForVedtaksperiode
import java.util.*
import javax.sql.DataSource

internal class SnapshotDao(private val dataSource: DataSource) {

    fun insertSpeilSnapshot(personBlob: String) = using(sessionOf(dataSource, returnGeneratedKey = true)) {
        it.insertSpeilSnapshot(personBlob)
    }

    fun oppdaterSnapshotForVedtaksperiode(vedtaksperiodeId: UUID, snapshot: String) =
        sessionOf(dataSource).use { it.oppdaterSnapshotForVedtaksperiode(vedtaksperiodeId, snapshot) }
}
