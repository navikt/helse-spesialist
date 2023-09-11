package no.nav.helse.db

import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language

class TildelingDao(private val dataSource: DataSource) {
    fun tildel(oppgaveId: Long, saksbehandlerOid: UUID, påVent: Boolean) {
        @Language("PostgreSQL")
        val query = """
            INSERT INTO tildeling(saksbehandler_ref, oppgave_id_ref, på_vent) VALUES (:oid, :oppgave_id, :pa_vent)
            ON CONFLICT (oppgave_id_ref) DO UPDATE SET saksbehandler_ref = :oid, på_vent = :pa_vent
        """

        sessionOf(dataSource).use {
            it.run(
                queryOf(
                    query, mapOf(
                        "oid" to saksbehandlerOid,
                        "oppgave_id" to oppgaveId,
                        "pa_vent" to påVent
                    )
                ).asUpdate
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