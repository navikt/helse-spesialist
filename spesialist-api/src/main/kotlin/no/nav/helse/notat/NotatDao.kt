package no.nav.helse.notat

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

class NotatDao(private val dataSource: DataSource) {

    fun opprettNotat(vedtaksperiodeId: UUID, tekst: String, saksbehandler_oid: UUID) = sessionOf(dataSource).use  { session ->
        @Language("PostgreSQL")
        val statement = """
                INSERT INTO notat (vedtaksperiode_id, tekst, saksbehandler_oid)
                VALUES (:vedtaksperiode_id, :tekst, :saksbehandler_oid);
        """
        session.run(queryOf(statement, mapOf(
            "vedtaksperiode_id" to vedtaksperiodeId,
            "tekst" to tekst,
            "saksbehandler_oid" to saksbehandler_oid
        )).asUpdate)
    }

    fun finnNotater(vedtaksperiodeIds: List<UUID>) = sessionOf(dataSource).use { session ->
        val questionMarks = vedtaksperiodeIds.joinToString { "?" }
        val values = vedtaksperiodeIds.toTypedArray()
        @Language("PostgreSQL")
        val statement = """
                SELECT * FROM notat n
                JOIN saksbehandler s on s.oid = n.saksbehandler_oid
                WHERE vedtaksperiode_id in ($questionMarks)
        """
        session.run(
            queryOf(statement, *values)
                .map(::notatDto).asList
        ).groupBy{ it.vedtaksperiodeId }
    }

    companion object {
        fun notatDto(it: Row) = NotatDto(
            id = it.int("id"),
            tekst = it.string("tekst"),
            opprettet = it.localDateTime("opprettet"),
            saksbehandlerOid = UUID.fromString(it.string("oid")),
            saksbehandlerNavn = it.string("navn"),
            saksbehandlerEpost = it.string("epost"),
            vedtaksperiodeId = UUID.fromString(it.string("vedtaksperiode_id")),
        )
    }
}
