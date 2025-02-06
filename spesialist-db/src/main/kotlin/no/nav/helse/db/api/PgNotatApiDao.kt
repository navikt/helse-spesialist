package no.nav.helse.db.api

import kotliquery.Row
import no.nav.helse.db.HelseDao.Companion.asSQL
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.QueryRunner
import no.nav.helse.db.api.NotatApiDao.NotatDto
import no.nav.helse.db.api.NotatApiDao.NotatType
import no.nav.helse.spesialist.api.notat.KommentarDto
import java.util.UUID
import javax.sql.DataSource

class PgNotatApiDao internal constructor(
    private val dataSource: DataSource,
) : QueryRunner by MedDataSource(dataSource), NotatApiDao {
    // PåVent-notater og Retur-notater lagres nå i periodehistorikk, og skal ikke være med til speil som en del av notater
    override fun finnNotater(vedtaksperiodeId: UUID): List<NotatDto> =
        asSQL(
            """ 
            SELECT * FROM notat n
            JOIN saksbehandler s on s.oid = n.saksbehandler_oid
            WHERE n.vedtaksperiode_id = :vedtaksperiode_id::uuid
            AND type not in ('PaaVent', 'Retur');
            """.trimIndent(),
            "vedtaksperiode_id" to vedtaksperiodeId,
        ).list { mapNotatDto(it) }

    override fun finnKommentarer(dialogRef: Long): List<KommentarDto> =
        asSQL(
            """
            select id, tekst, feilregistrert_tidspunkt, opprettet, saksbehandlerident
            from kommentarer
            where dialog_ref = :dialogRef
            """.trimIndent(),
            "dialogRef" to dialogRef,
        ).list { mapKommentarDto(it) }

    private fun mapNotatDto(it: Row): NotatDto =
        NotatDto(
            id = it.int("id"),
            dialogRef = it.long("dialog_ref").toInt(),
            tekst = it.string("tekst"),
            opprettet = it.localDateTime("opprettet"),
            saksbehandlerOid = UUID.fromString(it.string("oid")),
            saksbehandlerNavn = it.string("navn"),
            saksbehandlerEpost = it.string("epost"),
            saksbehandlerIdent = it.string("ident"),
            vedtaksperiodeId = UUID.fromString(it.string("vedtaksperiode_id")),
            feilregistrert = it.boolean("feilregistrert"),
            feilregistrert_tidspunkt = it.localDateTimeOrNull("feilregistrert_tidspunkt"),
            type = NotatType.valueOf(it.string("type")),
            kommentarer = finnKommentarer(it.long("dialog_ref")),
        )

    private fun mapKommentarDto(it: Row): KommentarDto =
        KommentarDto(
            id = it.int("id"),
            tekst = it.string("tekst"),
            opprettet = it.localDateTime("opprettet"),
            saksbehandlerident = it.string("saksbehandlerident"),
            feilregistrertTidspunkt = it.localDateTimeOrNull("feilregistrert_tidspunkt"),
        )
}
