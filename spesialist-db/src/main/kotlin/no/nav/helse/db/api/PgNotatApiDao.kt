package no.nav.helse.db.api

import kotliquery.Row
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.QueryRunner
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.notat.KommentarDto
import no.nav.helse.spesialist.api.notat.NotatDto
import java.util.UUID
import javax.sql.DataSource

class PgNotatApiDao internal constructor(
    private val dataSource: DataSource,
) : QueryRunner by MedDataSource(dataSource), NotatApiDao {
    override fun opprettNotat(
        vedtaksperiodeId: UUID,
        tekst: String,
        saksbehandlerOid: UUID,
        type: NotatType,
    ): NotatDto? =
        asSQL(
            """ 
            with dialog_ref AS (
                INSERT INTO dialog (opprettet)
                     VALUES (now())
                     RETURNING id
            ),
            inserted AS (
                INSERT INTO notat (vedtaksperiode_id, tekst, saksbehandler_oid, type, dialog_ref)
                VALUES (:vedtaksperiode_id, :tekst, :saksbehandler_oid, CAST(:type as notattype), (select id from dialog_ref))
                RETURNING *
            )
            SELECT * FROM inserted i INNER JOIN saksbehandler s ON i.saksbehandler_oid = s.oid
            """.trimIndent(),
            "vedtaksperiode_id" to vedtaksperiodeId,
            "tekst" to tekst,
            "saksbehandler_oid" to saksbehandlerOid,
            "type" to type.name,
        ).singleOrNull { mapNotatDto(it) }

    override fun leggTilKommentar(
        dialogRef: Int,
        tekst: String,
        saksbehandlerident: String,
    ): KommentarDto? =
        asSQL(
            """
            insert into kommentarer (tekst, saksbehandlerident, dialog_ref)
            values (:tekst, :saksbehandlerident, :dialogRef)
            returning *
            """.trimIndent(),
            "tekst" to tekst,
            "dialogRef" to dialogRef,
            "saksbehandlerident" to saksbehandlerident,
        ).singleOrNull { mapKommentarDto(it) }

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

    override fun feilregistrerNotat(notatId: Int): NotatDto? =
        asSQL(
            """ 
            WITH inserted AS (
                UPDATE notat
                SET feilregistrert = true, feilregistrert_tidspunkt = now()
                WHERE notat.id = :notatId
                RETURNING *
            )
            SELECT *
            FROM inserted i
            INNER JOIN saksbehandler s on s.oid = i.saksbehandler_oid
            """.trimIndent(),
            "notatId" to notatId,
        ).singleOrNull { mapNotatDto(it) }

    override fun feilregistrerKommentar(kommentarId: Int): KommentarDto? =
        asSQL(
            """
            WITH inserted AS (
                UPDATE kommentarer
                SET feilregistrert_tidspunkt = now()
                WHERE id = :kommentarId
                RETURNING *
            )
            SELECT *
            FROM inserted AS i
            INNER JOIN notat n on n.dialog_ref = i.dialog_ref
            """.trimIndent(),
            "kommentarId" to kommentarId,
        ).singleOrNull { mapKommentarDto(it) }

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
