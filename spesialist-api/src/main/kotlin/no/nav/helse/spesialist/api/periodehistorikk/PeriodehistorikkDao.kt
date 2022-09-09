package no.nav.helse.spesialist.api.periodehistorikk

import java.util.UUID
import javax.sql.DataSource
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import org.intellij.lang.annotations.Language

class PeriodehistorikkDao(private val dataSource: DataSource) : HelseDao(dataSource) {

    fun finn(utbetalingId: UUID) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement = """
                SELECT ph.id, ph.type, ph.timestamp, ph.notat_id, s.ident 
                FROM periodehistorikk ph
                LEFT JOIN saksbehandler s on ph.saksbehandler_oid = s.oid
                WHERE ph.utbetaling_id = :utbetaling_id
        """
        session.run(
            queryOf(statement, mapOf("utbetaling_id" to utbetalingId))
                .map(::periodehistorikkDto).asList
        )
    }

    fun lagre(historikkType: PeriodehistorikkType, saksbehandlerOid: UUID? = null, utbetalingId: UUID, notatId: Int? = null) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = """
                INSERT INTO periodehistorikk (type, saksbehandler_oid, utbetaling_id, notat_id)
                VALUES (:type, :saksbehandler_oid, :utbetaling_id, :notat_id)
        """
            session.run(
                queryOf(
                    statement,
                    mapOf(
                        "type" to historikkType.name,
                        "saksbehandler_oid" to saksbehandlerOid,
                        "utbetaling_id" to utbetalingId,
                        "notat_id" to notatId
                    )
                ).asUpdate
            )
        }

    fun migrer(tidligereUtbetalingId: UUID, utbetalingId: UUID) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = """
                UPDATE periodehistorikk 
                SET utbetaling_id = :utbetalingId
                WHERE utbetaling_id = :tidligereUtbetalingId
        """
            session.run(
                queryOf(
                    statement,
                    mapOf(
                        "utbetalingId" to utbetalingId,
                        "tidligereUtbetalingId" to tidligereUtbetalingId,
                    )
                ).asUpdate
            )
        }

    companion object {
        fun periodehistorikkDto(it: Row) = PeriodehistorikkDto(
            id = it.int("id"),
            type = PeriodehistorikkType.valueOf(it.string("type")),
            timestamp = it.localDateTime("timestamp"),
            saksbehandler_ident = it.stringOrNull("ident"),
            notat_id = it.intOrNull("notat_id")
        )
    }
}
