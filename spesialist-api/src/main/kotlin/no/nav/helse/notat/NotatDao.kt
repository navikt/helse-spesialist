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

    fun finnNotater(oppgave_ref: Int) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement = """
                SELECT * FROM notat n
                JOIN saksbehandler s on s.oid = n.saksbehandler_oid
                WHERE oppgave_ref = ?
                """
        session.run(
            queryOf(statement, oppgave_ref)
                .map (::notatDto).asList
        )
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
