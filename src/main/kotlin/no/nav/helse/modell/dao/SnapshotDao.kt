package no.nav.helse.modell.dao

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
                    "INSERT INTO speil_snapshot(data) VALUES(?);",
                    personBlob
                ).asUpdateAndReturnGeneratedKey
            )
        }?.toInt())
}
