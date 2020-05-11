package no.nav.helse.modell.vedtak.snapshot

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import javax.sql.DataSource

class SnapshotDao(private val dataSource: DataSource) {
    internal fun insertSpeilSnapshot(
        personBlob: String
    ): Int =
        requireNotNull(using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO speil_snapshot(data) VALUES(CAST(? as json));",
                    personBlob
                ).asUpdateAndReturnGeneratedKey
            )
        }?.toInt())

    internal fun findSpeilSnapshot(
        id: Long
    ): String? =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT data FROM speil_snapshot WHERE id=?;",
                    id
                ).map { it.string("data") }.asSingle
            )
        }
}
