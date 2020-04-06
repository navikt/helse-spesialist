package no.nav.helse.modell.dao

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.util.*
import javax.sql.DataSource

internal class SpleisbehovDao(private val dataSource: DataSource) {

    internal fun insertBehov(id: UUID, behov: String) {
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO spleisbehov(id, data) VALUES(?, CAST(? as json))", id, behov
                ).asUpdate
            )

        }
    }

    internal fun updateBehov(id: UUID, behov: String) {
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "UPDATE spleisbehov SET data=CAST(? as json) WHERE id=?", behov, id
                ).asUpdate
            )

        }
    }

    internal fun findBehov(id: UUID): String? {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT * FROM spleisbehov WHERE id=?", id
                ).map { it.string("data") }
                    .asSingle
            )
        }
    }
}
