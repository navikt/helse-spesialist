package no.nav.helse.db

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.util.UUID
import javax.sql.DataSource

class TildelingDao(private val dataSource: DataSource) {
    fun tildel(
        oppgaveId: Long,
        saksbehandlerOid: UUID,
    ) {
        @Language("PostgreSQL")
        val query = """
            INSERT INTO tildeling(saksbehandler_ref, oppgave_id_ref) VALUES (:oid, :oppgave_id)
            ON CONFLICT (oppgave_id_ref) DO UPDATE SET saksbehandler_ref = :oid
        """

        sessionOf(dataSource).use {
            it.run(
                queryOf(
                    query,
                    mapOf(
                        "oid" to saksbehandlerOid,
                        "oppgave_id" to oppgaveId,
                    ),
                ).asUpdate,
            )
        }
    }

    fun avmeld(oppgaveId: Long) {
        @Language("PostgreSQL")
        val query = """
            DELETE FROM tildeling WHERE oppgave_id_ref = :oppgave_id
        """

        sessionOf(dataSource).use {
            it.run(queryOf(query, mapOf("oppgave_id" to oppgaveId)).asUpdate)
        }
    }
}
