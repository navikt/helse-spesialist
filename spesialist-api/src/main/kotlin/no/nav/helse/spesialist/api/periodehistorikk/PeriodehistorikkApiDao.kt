package no.nav.helse.spesialist.api.periodehistorikk

import kotliquery.Row
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.QueryRunner
import javax.sql.DataSource

class PeriodehistorikkApiDao(
    private val dataSource: DataSource,
) : QueryRunner by MedDataSource(dataSource) {
    fun hentForPerson(fødselsnummer: String) =
        asSQL(
            """
            with generasjon_id_og_utbetaling_id as (
                select b.utbetaling_id, b.unik_id as generasjon_id
                from behandling b
                join vedtak v on b.vedtaksperiode_id = v.vedtaksperiode_id
                join person p on v.person_ref = p.id
                where p.fødselsnummer = :foedselsnummer
            )
            select id, type, timestamp, notat_id, json, ident, dialog_ref, coalesce(utvalg.utbetaling_id, ph.utbetaling_id) as utbetaling_id, utvalg.generasjon_id
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
