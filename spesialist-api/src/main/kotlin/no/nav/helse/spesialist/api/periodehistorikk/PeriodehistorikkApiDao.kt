package no.nav.helse.spesialist.api.periodehistorikk

import kotliquery.Row
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.QueryRunner
import java.util.UUID
import javax.sql.DataSource

class PeriodehistorikkApiDao(
    private val dataSource: DataSource,
) : QueryRunner by MedDataSource(dataSource) {
    fun finn(utbetalingId: UUID) =
        asSQL(
            // Bruker UNION som en quick fix for å unngå seq scan på en spørring som gjøres mange ganger når
            // GraphQL skal bygge person-dataene.
            // Må kaste til jsonb for at databasen skal klare å filtrere vekk duplikate rader
            """
            SELECT ph.id, ph.type, ph.timestamp, ph.notat_id, ph.json::jsonb, s.ident, ph.dialog_ref
            FROM periodehistorikk ph
            LEFT JOIN saksbehandler s ON ph.saksbehandler_oid = s.oid
            WHERE ph.utbetaling_id = :utbetaling_id
            UNION
            SELECT ph.id, ph.type, ph.timestamp, ph.notat_id, ph.json::jsonb, s.ident, ph.dialog_ref
            FROM periodehistorikk ph
            LEFT JOIN saksbehandler s ON ph.saksbehandler_oid = s.oid
            LEFT JOIN selve_vedtaksperiode_generasjon svg ON ph.generasjon_id = svg.unik_id
            WHERE svg.utbetaling_id = :utbetaling_id
            """.trimIndent(),
            "utbetaling_id" to utbetalingId,
        ).list { periodehistorikkDto(it) }

    companion object {
        fun periodehistorikkDto(it: Row) =
            PeriodehistorikkDto(
                id = it.int("id"),
                type = PeriodehistorikkType.valueOf(it.string("type")),
                timestamp = it.localDateTime("timestamp"),
                saksbehandlerIdent = it.stringOrNull("ident"),
                notatId = it.intOrNull("notat_id"),
                dialogRef = it.longOrNull("dialog_ref")?.toInt(),
                json = it.string("json"),
            )
    }
}
