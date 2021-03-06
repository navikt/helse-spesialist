package no.nav.helse.tildeling

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class TildelingDao(private val dataSource: DataSource) {

    fun opprettTildeling(oppgaveId: Long, saksbehandleroid: UUID, gyldigTil: LocalDateTime? = null): Boolean {
        return sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = """
                INSERT INTO tildeling (oppgave_id_ref, saksbehandler_ref, gyldig_til)
                VALUES (:oppgave_id_ref, :saksbehandler_ref, :gyldig_til);
            """.trimIndent()
            session.transaction { tx ->
                if (tx.tildelingForOppgave(oppgaveId) != null) false
                else {
                    tx.run(
                        queryOf(
                            query, mapOf(
                                "oppgave_id_ref" to oppgaveId,
                                "saksbehandler_ref" to saksbehandleroid,
                                "gyldig_til" to gyldigTil
                            )
                        ).asUpdate
                    )
                    true
                }
            }
        }
    }

    fun slettTildeling(oppgaveId: Long) {
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = """
                DELETE
                FROM tildeling
                WHERE oppgave_id_ref = :oppgave_id_ref;
            """.trimIndent()
            session.run(queryOf(query, mapOf("oppgave_id_ref" to oppgaveId)).asUpdate)
        }
    }

    fun fjernPåVent(oppgaveId: Long) {
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = """
                UPDATE tildeling
                SET på_vent = false
                WHERE oppgave_id_ref = :oppgave_id_ref;
            """.trimIndent()
            session.run(queryOf(query, mapOf("oppgave_id_ref" to oppgaveId)).asUpdate)
        }
    }

    fun tildelingForPerson(fødselsnummer: String) = sessionOf(dataSource).use {
        @Language("PostgreSQL")
        val query = """
            SELECT s.epost, s.oid, s.navn, t.på_vent FROM person
                 RIGHT JOIN vedtak v on person.id = v.person_ref
                 RIGHT JOIN oppgave o on v.id = o.vedtak_ref
                 RIGHT JOIN tildeling t on o.id = t.oppgave_id_ref AND (t.gyldig_til IS NULL OR t.gyldig_til > now())
                 RIGHT JOIN saksbehandler s on t.saksbehandler_ref = s.oid
            WHERE fodselsnummer = ? AND o.status = 'AvventerSaksbehandler'
            ORDER BY o.opprettet DESC;
        """
        it.run(queryOf(query, fødselsnummer.toLong()).map(::tildelingDto).asSingle)
    }

    fun leggOppgavePåVent(oppgaveId: Long) {
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = """
                UPDATE tildeling
                SET på_vent = true
                WHERE oppgave_id_ref = :oppgave_id_ref;
            """.trimIndent()
            session.run(
                queryOf(query, mapOf("oppgave_id_ref" to oppgaveId)).asUpdate
            )
        }
    }

    private fun tildelingDto(it: Row) = TildelingApiDto(
        epost = it.string("epost"),
        påVent = it.boolean("på_vent"),
        oid = UUID.fromString(it.string("oid")),
        navn = it.string("navn")
    )

    fun tildelingForOppgave(oppgaveId: Long): TildelingApiDto? =
        sessionOf(dataSource).use { it.tildelingForOppgave(oppgaveId) }

    private fun Session.tildelingForOppgave(oppgaveId: Long): TildelingApiDto? {
        @Language("PostgreSQL")
        val query = """
            SELECT s.oid, s.epost, s.navn, t.på_vent FROM tildeling t
                INNER JOIN saksbehandler s on s.oid = t.saksbehandler_ref
                INNER JOIN oppgave o on t.oppgave_id_ref = o.id
            WHERE o.id = :oppgaveId
            AND (t.gyldig_til IS NULL OR t.gyldig_til > now())
            """
        return run(queryOf(query, mapOf("oppgaveId" to oppgaveId)).map(::tildelingDto).asSingle)
    }
}
