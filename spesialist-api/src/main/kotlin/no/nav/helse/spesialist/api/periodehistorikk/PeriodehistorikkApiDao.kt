package no.nav.helse.spesialist.api.periodehistorikk

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import org.intellij.lang.annotations.Language
import java.util.UUID
import javax.sql.DataSource

class PeriodehistorikkApiDao(private val dataSource: DataSource) : HelseDao(dataSource) {
    fun finn(utbetalingId: UUID) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement = """
                SELECT ph.id, ph.type, ph.timestamp, ph.notat_id, ph.json, s.ident 
                FROM periodehistorikk ph
                LEFT JOIN saksbehandler s on ph.saksbehandler_oid = s.oid
                WHERE ph.utbetaling_id = :utbetaling_id OR ph.generasjon_id IN (SELECT unik_id FROM selve_vedtaksperiode_generasjon svg WHERE svg.utbetaling_id = :utbetaling_id)
        """
            session.run(
                queryOf(statement, mapOf("utbetaling_id" to utbetalingId))
                    .map(::periodehistorikkDto).asList,
            )
        }

    companion object {
        fun periodehistorikkDto(it: Row) =
            PeriodehistorikkDto(
                id = it.int("id"),
                type = PeriodehistorikkType.valueOf(it.string("type")),
                timestamp = it.localDateTime("timestamp"),
                saksbehandler_ident = it.stringOrNull("ident"),
                notat_id = it.intOrNull("notat_id"),
                json = it.string("json"),
            )
    }
}
