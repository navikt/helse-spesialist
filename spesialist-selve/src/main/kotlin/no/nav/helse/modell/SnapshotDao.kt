package no.nav.helse.modell

import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

internal class SnapshotDao(private val dataSource: DataSource) {

    fun insertSpeilSnapshot(personBlob: String) = using(sessionOf(dataSource, returnGeneratedKey = true)) {
        it.insertSpeilSnapshot(personBlob).toInt()
    }

    fun oppdaterSnapshotForVedtaksperiode(vedtaksperiodeId: UUID, snapshot: String) =
        sessionOf(dataSource).use { it.oppdaterSnapshotForVedtaksperiode(vedtaksperiodeId, snapshot) }

    private fun Session.insertSpeilSnapshot(personBlob: String): Long {
        @Language("PostgreSQL")
        val statement = "INSERT INTO speil_snapshot(data) VALUES(CAST(:personBlob as json));"
        return requireNotNull(
            this.run(
                queryOf(
                    statement,
                    mapOf("personBlob" to personBlob)
                ).asUpdateAndReturnGeneratedKey
            )
        )
    }

    internal fun findSpeilSnapshot(id: Int) = using(sessionOf(dataSource)) { it.findSpeilSnapshot(id) }

    private fun Session.findSpeilSnapshot(id: Int): String? {
        @Language("PostgreSQL")
        val statement = "SELECT data FROM speil_snapshot WHERE id=:id;"
        return this.run(
            queryOf(
                statement,
                mapOf("id" to id)
            ).map { it.string("data") }.asSingle
        )
    }

    private fun Session.oppdaterSnapshotForVedtaksperiode(vedtaksperiodeId: UUID, snapshot: String): Int {
        @Language("PostgreSQL")
        val statement = """
        UPDATE speil_snapshot
        SET data=CAST(:snapshot as json), sist_endret=now()
        WHERE id = (SELECT speil_snapshot_ref FROM vedtak WHERE vedtaksperiode_id=:vedtaksperiodeId);
    """
        return requireNotNull(
            this.run(
                queryOf(
                    statement,
                    mapOf(
                        "snapshot" to snapshot,
                        "vedtaksperiodeId" to vedtaksperiodeId
                    )
                ).asUpdate
            )
        )
    }
}
