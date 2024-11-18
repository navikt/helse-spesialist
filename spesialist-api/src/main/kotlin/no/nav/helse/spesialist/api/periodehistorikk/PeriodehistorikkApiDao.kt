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
            // Fiklete greier, for å unngå sequential scan. Spørringen gjøres mange ganger når GraphQL skal bygge
            // person-dataene.
            """
            WITH innslag_med_utbetaling_id AS
            (
                SELECT id FROM periodehistorikk WHERE utbetaling_id = :utbetalingId
            ),
            innslag_via_selve_vedtaksperiode_generasjon AS
            (
                SELECT ph.id
                FROM periodehistorikk ph
                JOIN selve_vedtaksperiode_generasjon svg ON generasjon_id = unik_id
                WHERE svg.utbetaling_id = :utbetaling_id
            )
            SELECT id, type, timestamp, notat_id, json, ident, dialog_ref, utbetaling_id, generasjon_id
            FROM periodehistorikk
            LEFT JOIN saksbehandler s ON saksbehandler_oid = s.oid
            WHERE id IN
            (
                SELECT id FROM innslag_med_utbetaling_id
                UNION ALL
                SELECT id FROM innslag_via_selve_vedtaksperiode_generasjon
            )
            """.trimIndent(),
            "utbetaling_id" to utbetalingId,
        ).list { periodehistorikkDto(it) }

    fun hentForPerson(fødselsnummer: String) =
        asSQL(
            """
            with generasjon_id_og_utbetaling_id as (
                select svg.utbetaling_id, svg.unik_id as generasjon_id
                from selve_vedtaksperiode_generasjon svg
                join vedtak v on svg.vedtaksperiode_id = v.vedtaksperiode_id
                join person p on v.person_ref = p.id
                where p.fødselsnummer = :foedselsnummer
            )
            select id, type, timestamp, notat_id, json, ident, dialog_ref, utvalg.utbetaling_id, utvalg.generasjon_id
            from generasjon_id_og_utbetaling_id utvalg
            left join periodehistorikk ph on utvalg.utbetaling_id = ph.utbetaling_id or utvalg.generasjon_id = ph.generasjon_id
            left join saksbehandler s on saksbehandler_oid = s.oid
            where ph.id is not null
            """.trimIndent(),
            "foedselsnummer" to fødselsnummer,
        ).list { it.uuid("utbetaling_id") to periodehistorikkDto(it) }.groupBy({ it.first }) { it.second }

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
