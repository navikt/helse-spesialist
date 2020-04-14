package no.nav.helse.modell.dao

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.util.UUID
import javax.sql.DataSource

internal class SpleisbehovDao(private val dataSource: DataSource) {

    internal fun insertBehov(id: UUID, behov: String, original: String) {
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO spleisbehov(id, data, original) VALUES(?, CAST(? as json), CAST(? as json))",
                    id,
                    behov,
                    original
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

    internal fun findBehov(id: UUID): String? = using(sessionOf(dataSource)) { session ->
        session.run(queryOf("SELECT data FROM spleisbehov WHERE id=?", id)
            .map { it.string("data") }
            .asSingle
        )
    }

    internal fun findOriginalBehov(id: UUID): String? = using(sessionOf(dataSource)) { session ->
        session.run(queryOf("SELECT original FROM spleisbehov WHERE id=?", id)
            .map { it.string("original") }
            .asSingle)
    }
}
