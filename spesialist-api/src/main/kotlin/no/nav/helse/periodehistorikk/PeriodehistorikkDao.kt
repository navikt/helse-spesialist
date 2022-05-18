package no.nav.helse.periodehistorikk

import java.util.UUID
import javax.sql.DataSource
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import org.intellij.lang.annotations.Language

class PeriodehistorikkDao(private val dataSource: DataSource) : HelseDao(dataSource) {

    fun finn(periodeId: UUID) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement = """
                SELECT ph.id, ph.type, ph.timestamp, ph.notat_id, s.ident FROM periodehistorikk ph
                JOIN saksbehandler s on ph.saksbehandler_oid = s.oid
                WHERE ph.periode_id = :periode_id
        """
        session.run(
            queryOf(statement, mapOf("periode_id" to periodeId))
                .map(::periodehistorikkDto).asList
        )
    }

    fun lagre(historikkType: PeriodehistorikkType, saksbehandlerOid: UUID, periodeId: UUID, notatId: Int? = null) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = """
                INSERT INTO periodehistorikk (type, saksbehandler_oid, periode_id, notat_id)
                VALUES (:type, :saksbehandler_oid, :periode_id, :notat_id)
        """
            session.run(
                queryOf(
                    statement,
                    mapOf(
                        "type" to historikkType.name,
                        "saksbehandler_oid" to saksbehandlerOid,
                        "periode_id" to periodeId,
                        "notat_id" to notatId
                    )
                ).asUpdate
            )
        }

    companion object {
        fun periodehistorikkDto(it: Row) = PeriodehistorikkDto(
            id = it.int("id"),
            type = PeriodehistorikkType.valueOf(it.string("type")),
            timestamp = it.localDateTime("timestamp"),
            saksbehandler_ident = it.string("ident"),
            notat_id = it.intOrNull("notat_id")
        )
    }
}
