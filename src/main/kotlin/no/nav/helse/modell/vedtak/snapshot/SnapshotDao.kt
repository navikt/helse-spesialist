package no.nav.helse.modell.vedtak.snapshot

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.util.*
import javax.sql.DataSource

class SnapshotDao(private val dataSource: DataSource) {
    internal fun insertSpeilSnapshot(personBlob: String): Int =
        requireNotNull(using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO speil_snapshot(data) VALUES(CAST(? as json));",
                    personBlob
                ).asUpdateAndReturnGeneratedKey
            )
        }?.toInt())

    internal fun findSpeilSnapshot(id: Int): String? =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT data FROM speil_snapshot WHERE id=?;",
                    id
                ).map { it.string("data") }.asSingle
            )
        }

    fun oppdaterSnapshotForVedtaksperiode(vedtaksperiodeId: UUID, snapshot: String): Int =
        requireNotNull(using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    """
                            UPDATE speil_snapshot
                            SET data=CAST(? as json)
                            WHERE id = (SELECT speil_snapshot_ref FROM vedtak WHERE vedtaksperiode_id=?);
                        """,
                    snapshot,
                    vedtaksperiodeId
                ).asUpdate
            )
        }.toInt())
}
