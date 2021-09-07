package no.nav.helse.notat

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

class NotatDao(private val dataSource: DataSource) : HelseDao(dataSource) {

    fun opprettNotat(vedtaksperiodeId: UUID, tekst: String, saksbehandler_oid: UUID) =
        """ INSERT INTO notat (vedtaksperiode_id, tekst, saksbehandler_oid)
            VALUES (:vedtaksperiode_id, :tekst, :saksbehandler_oid);
        """.update(
            mapOf(
                "vedtaksperiode_id" to vedtaksperiodeId,
                "tekst" to tekst,
                "saksbehandler_oid" to saksbehandler_oid
            )
        )

    fun finnNotat(id: Int) =
        """ SELECT * FROM notat n
            JOIN saksbehandler s on s.oid = n.saksbehandler_oid
            WHERE id = :id
        """.single(mapOf("id" to id)) { notatDto(it) }

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
        ).groupBy { it.vedtaksperiodeId }
    }

    fun feilregistrer(notatId: Int, saksbehandler_oid: UUID) =
        """ UPDATE notat
            SET feilregistrert = true
            WHERE notat.id = :notatId
        """.update(mapOf("notatId" to notatId, "saksbehandler_oid" to saksbehandler_oid))

    companion object {
        fun notatDto(it: Row) = NotatDto(
            id = it.int("id"),
            tekst = it.string("tekst"),
            opprettet = it.localDateTime("opprettet"),
            saksbehandlerOid = UUID.fromString(it.string("oid")),
            saksbehandlerNavn = it.string("navn"),
            saksbehandlerEpost = it.string("epost"),
            vedtaksperiodeId = UUID.fromString(it.string("vedtaksperiode_id")),
            feilregistrert = it.boolean("feilregistrert")
        )
    }
}
