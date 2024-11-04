package no.nav.helse.spesialist.api.notat

import kotliquery.Row
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.HelseDao.Companion.asSQLWithQuestionMarks
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.QueryRunner
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import java.util.UUID
import javax.sql.DataSource

class NotatApiDao(private val dataSource: DataSource) : QueryRunner by MedDataSource(dataSource) {
    fun opprettNotat(
        vedtaksperiodeId: UUID,
        tekst: String,
        saksbehandlerOid: UUID,
        type: NotatType = NotatType.Generelt,
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

    fun leggTilKommentar(
        notatId: Int,
        tekst: String,
        saksbehandlerident: String,
    ): KommentarDto? =
        asSQL(
            """
            insert into kommentarer (tekst, notat_ref, saksbehandlerident, dialog_ref)
            values (:tekst, :notatId, :saksbehandlerident, (select dialog_ref from notat where id = :notatId))
            returning *
            """.trimIndent(),
            "tekst" to tekst,
            "notatId" to notatId,
            "saksbehandlerident" to saksbehandlerident,
        ).singleOrNull { mapKommentarDto(it) }

    fun finnNotater(vedtaksperiodeId: UUID): List<NotatDto> =
        asSQL(
            """ 
            SELECT * FROM notat n
            JOIN saksbehandler s on s.oid = n.saksbehandler_oid
            WHERE n.vedtaksperiode_id = :vedtaksperiode_id::uuid;
            """.trimIndent(),
            "vedtaksperiode_id" to vedtaksperiodeId,
        ).list { mapNotatDto(it) }

    fun finnNotater(vedtaksperiodeIds: List<UUID>): Map<UUID, List<NotatDto>> {
        return asSQLWithQuestionMarks(
            """
            SELECT * FROM notat n
            JOIN saksbehandler s on s.oid = n.saksbehandler_oid
            WHERE vedtaksperiode_id in (${vedtaksperiodeIds.joinToString { "?" }})
            """.trimIndent(),
            *vedtaksperiodeIds.toTypedArray(),
        ).list { mapNotatDto(it) }.groupBy { it.vedtaksperiodeId }
    }

    fun feilregistrerNotat(notatId: Int): NotatDto? =
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

    fun feilregistrerKommentar(kommentarId: Int): KommentarDto? =
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
            INNER JOIN notat n on n.id = i.notat_ref
            """.trimIndent(),
            "kommentarId" to kommentarId,
        ).singleOrNull { mapKommentarDto(it) }

    private fun finnKommentarer(notatId: Int): List<KommentarDto> =
        asSQL(
            """
            select k.id, k.tekst, k.feilregistrert_tidspunkt, k.opprettet, k.saksbehandlerident
            from kommentarer k
            inner join notat n on n.id = k.notat_ref
            where n.id = :notatId
            """.trimIndent(),
            "notatId" to notatId,
        ).list { mapKommentarDto(it) }

    private fun mapNotatDto(it: Row): NotatDto =
        NotatDto(
            id = it.int("id"),
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
            kommentarer = finnKommentarer(it.int("id")),
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
