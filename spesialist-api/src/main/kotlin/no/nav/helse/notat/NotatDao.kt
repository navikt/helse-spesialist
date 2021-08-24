package no.nav.helse.notat

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

class NotatDao(private val dataSource: DataSource) {

    fun opprettNotat(oppgave_ref: Int, tekst: String, saksbehandler_oid: UUID) = sessionOf(dataSource).use  { session ->
        @Language("PostgreSQL")
        val statement = """
                INSERT INTO notat (oppgave_ref, tekst, saksbehandler_oid)
                VALUES (:oppgave_ref, :tekst, :saksbehandler_oid);
        """
        session.run(queryOf(statement, mapOf(
            "oppgave_ref" to oppgave_ref,
            "tekst" to tekst,
            "saksbehandler_oid" to saksbehandler_oid
        )).asUpdate)
    }

    fun finnNotater(oppgave_refs: List<Int>) = sessionOf(dataSource).use { session ->
        val questionMarks = oppgave_refs.map { "?" }.joinToString()
        val values = oppgave_refs.toTypedArray()
        @Language("PostgreSQL")
        val statement = """
                SELECT * FROM notat n
                JOIN saksbehandler s on s.oid = n.saksbehandler_oid
                WHERE oppgave_ref in ($questionMarks)
                """
        session.run(
            queryOf(statement, *values)
                .map (::notatDto).asList
        ).groupBy{ it.oppgaveRef }
    }

    companion object {
        fun notatDto(it: Row) = NotatDto(
            id = it.int("id"),
            tekst = it.string("tekst"),
            opprettet = it.localDateTime("opprettet"),
            saksbehandlerOid = UUID.fromString(it.string("saksbehandler_oid")),
            saksbehandlerNavn = it.string("navn"),
            saksbehandlerEpost = it.string("epost"),
            oppgaveRef = it.int("oppgave_ref")
        )
    }
}
