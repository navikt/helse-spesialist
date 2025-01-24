package no.nav.helse.db.api

import kotliquery.Row
import no.nav.helse.db.HelseDao.Companion.asSQL
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.QueryRunner
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDto
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import java.util.UUID
import javax.sql.DataSource

class PgPeriodehistorikkApiDao internal constructor(
    private val dataSource: DataSource,
) : QueryRunner by MedDataSource(dataSource), PeriodehistorikkApiDao {
    override fun finn(utbetalingId: UUID) =
        asSQL(
            // Fiklete greier, for å unngå sequential scan. Spørringen gjøres mange ganger når GraphQL skal bygge
            // person-dataene.
            """
            WITH innslag_med_utbetaling_id AS
            (
                SELECT id FROM periodehistorikk WHERE utbetaling_id = :utbetalingId
            ),
            innslag_via_behandling AS
            (
                SELECT ph.id
                FROM periodehistorikk ph
                JOIN behandling b ON generasjon_id = unik_id
                WHERE b.utbetaling_id = :utbetaling_id
            )
            SELECT id, type, timestamp, json, ident, dialog_ref, utbetaling_id, generasjon_id
            FROM periodehistorikk
            LEFT JOIN saksbehandler s ON saksbehandler_oid = s.oid
            WHERE id IN
            (
                SELECT id FROM innslag_med_utbetaling_id
                UNION ALL
                SELECT id FROM innslag_via_behandling
            )
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
                dialogRef = it.longOrNull("dialog_ref")?.toInt(),
                json = it.string("json"),
            )
    }
}
