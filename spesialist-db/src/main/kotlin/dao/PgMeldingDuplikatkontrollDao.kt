package no.nav.helse.spesialist.db.dao

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.db.MeldingDuplikatkontrollDao
import org.intellij.lang.annotations.Language
import java.util.UUID
import javax.sql.DataSource

class PgMeldingDuplikatkontrollDao internal constructor(
    private val dataSource: DataSource,
) : MeldingDuplikatkontrollDao {
    override fun lagre(
        meldingId: UUID,
        type: String,
    ) {
        @Language("PostgreSQL")
        val query =
            """
            INSERT INTO melding_duplikatkontroll(melding_id, type)
            VALUES(:meldingId, :type)
            ON CONFLICT DO NOTHING
            """.trimIndent()
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "meldingId" to meldingId,
                        "type" to type,
                    ),
                ).asUpdate,
            )
        }
    }

    override fun erBehandlet(meldingId: UUID): Boolean {
        @Language("PostgreSQL")
        val query =
            """
            select true from melding_duplikatkontroll
            where melding_id = :meldingId
            """.trimIndent()
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf("meldingId" to meldingId),
                ).map { it.boolean(1) }.asSingle,
            )
        } ?: false
    }
}
