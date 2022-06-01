package no.nav.helse.notat

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

class NotatDao(private val dataSource: DataSource) : HelseDao(dataSource) {

    fun opprettNotat(vedtaksperiodeId: UUID, tekst: String, saksbehandler_oid: UUID, type: NotatType = NotatType.Generelt) =
        """ INSERT INTO notat (vedtaksperiode_id, tekst, saksbehandler_oid, type)
            VALUES (:vedtaksperiode_id, :tekst, :saksbehandler_oid, CAST(:type as notattype));
        """.update(
            mapOf(
                "vedtaksperiode_id" to vedtaksperiodeId,
                "tekst" to tekst,
                "saksbehandler_oid" to saksbehandler_oid,
                "type" to type.name
            )
        )

    fun opprettNotatForOppgaveId(oppgaveId: Long, tekst: String, saksbehandler_oid: UUID, type: NotatType = NotatType.Generelt) =
        """ INSERT INTO notat (vedtaksperiode_id, tekst, saksbehandler_oid, type)
            VALUES (
                (SELECT v.vedtaksperiode_id 
                    FROM vedtak v 
                    INNER JOIN oppgave o on v.id = o.vedtak_ref 
                    WHERE o.id = :oppgave_id), 
                :tekst, 
                :saksbehandler_oid,
                CAST(:type as notattype)
            );
        """.update(
            mapOf(
                "oppgave_id" to oppgaveId,
                "tekst" to tekst,
                "saksbehandler_oid" to saksbehandler_oid,
                "type" to type.name
            )
        )

    fun finnNotat(id: Int) =
        """ SELECT * FROM notat n
            JOIN saksbehandler s on s.oid = n.saksbehandler_oid
            WHERE n.id = :id
        """.single(mapOf("id" to id)) { notatDto(it) }

    fun finnNotater(vedtaksperiodeId: UUID) =
        """ SELECT * FROM notat n
            JOIN saksbehandler s on s.oid = n.saksbehandler_oid
            WHERE n.vedtaksperiode_id=:vedtaksperiode_id
        """.list(mapOf("vedtaksperiode_id" to vedtaksperiodeId)) { notatDto(it) }

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
            SET feilregistrert = true, feilregistrert_tidspunkt = now()
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
            feilregistrert = it.boolean("feilregistrert"),
            feilregistrert_tidspunkt = it.localDateTimeOrNull("feilregistrert_tidspunkt"),
            type = NotatType.valueOf(it.string("type"))
        )
    }
}
