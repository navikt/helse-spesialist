package no.nav.helse.spesialist.api.notat

import java.util.UUID
import javax.sql.DataSource
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import org.intellij.lang.annotations.Language

class NotatDao(private val dataSource: DataSource) : HelseDao(dataSource) {

    fun opprettNotat(
        vedtaksperiodeId: UUID,
        tekst: String,
        saksbehandlerOid: UUID,
        type: NotatType = NotatType.Generelt,
    ): NotatDto? = asSQL(
        """ 
            with inserted AS (
                INSERT INTO notat (vedtaksperiode_id, tekst, saksbehandler_oid, type)
                VALUES (:vedtaksperiode_id, :tekst, :saksbehandler_oid, CAST(:type as notattype))
                RETURNING *
            )
            SELECT * FROM inserted i INNER JOIN saksbehandler s ON i.saksbehandler_oid = s.oid
        """, mapOf(
            "vedtaksperiode_id" to vedtaksperiodeId,
            "tekst" to tekst,
            "saksbehandler_oid" to saksbehandlerOid,
            "type" to type.name
        )
    ).single { mapNotatDto(it) }

    fun leggTilKommentar(notatId: Int, tekst: String, saksbehandlerident: String): KommentarDto? = asSQL(
        """
            insert into kommentarer (tekst, notat_ref, saksbehandlerident)
            values (:tekst, :notatId, :saksbehandlerident)
            returning *
        """, mapOf("tekst" to tekst, "notatId" to notatId, "saksbehandlerident" to saksbehandlerident)
    ).single { mapKommentarDto(it) }

    fun opprettNotatForOppgaveId(
        oppgaveId: Long,
        tekst: String,
        saksbehandlerOid: UUID,
        type: NotatType = NotatType.Generelt,
    ): Long? = asSQL(
        """ 
            INSERT INTO notat (vedtaksperiode_id, tekst, saksbehandler_oid, type)
            VALUES (
                (SELECT v.vedtaksperiode_id 
                    FROM vedtak v 
                    INNER JOIN oppgave o on v.id = o.vedtak_ref 
                    WHERE o.id = :oppgave_id), 
                :tekst, 
                :saksbehandler_oid,
                CAST(:type as notattype)
            );
        """, mapOf(
            "oppgave_id" to oppgaveId,
            "tekst" to tekst,
            "saksbehandler_oid" to saksbehandlerOid,
            "type" to type.name
        )
    ).updateAndReturnGeneratedKey()

    fun finnNotater(vedtaksperiodeId: UUID): List<NotatDto> = asSQL(
        """ 
            SELECT * FROM notat n
            JOIN saksbehandler s on s.oid = n.saksbehandler_oid
            WHERE n.vedtaksperiode_id = :vedtaksperiode_id::uuid;
        """, mapOf("vedtaksperiode_id" to vedtaksperiodeId)
    ).list { mapNotatDto(it) }

    fun finnNotater(vedtaksperiodeIds: List<UUID>): Map<UUID, List<NotatDto>> = sessionOf(dataSource).use { session ->
        val questionMarks = vedtaksperiodeIds.joinToString { "?" }
        val values = vedtaksperiodeIds.toTypedArray()

        @Language("PostgreSQL")
        val statement = """
                SELECT * FROM notat n
                JOIN saksbehandler s on s.oid = n.saksbehandler_oid
                WHERE vedtaksperiode_id in ($questionMarks)
        """
        session.run(
            queryOf(statement, *values)
                .map(::mapNotatDto).asList
        ).groupBy { it.vedtaksperiodeId }
    }


    fun feilregistrerNotat(notatId: Int): NotatDto? = asSQL(
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
        """, mapOf("notatId" to notatId)
    ).single(::mapNotatDto)

    fun feilregistrerKommentar(kommentarId: Int): KommentarDto? = asSQL(
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
        """, mapOf("kommentarId" to kommentarId)
    ).single(::mapKommentarDto )

    private fun finnKommentarer(notatId: Int): List<KommentarDto> = asSQL(
        """
            select k.id, k.tekst, k.feilregistrert_tidspunkt, k.opprettet, k.saksbehandlerident
            from kommentarer k
            inner join notat n on n.id = k.notat_ref
            where n.id = :notatId
        """, mapOf("notatId" to notatId)
    ).list { mapKommentarDto(it) }

    private fun mapNotatDto(it: Row): NotatDto = NotatDto(
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

    private fun mapKommentarDto(it: Row): KommentarDto = KommentarDto(
        id = it.int("id"),
        tekst = it.string("tekst"),
        opprettet = it.localDateTime("opprettet"),
        saksbehandlerident = it.string("saksbehandlerident"),
        feilregistrertTidspunkt = it.localDateTimeOrNull("feilregistrert_tidspunkt"),
    )

}
