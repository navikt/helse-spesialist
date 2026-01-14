package no.nav.helse.spesialist.db.dao

import no.nav.helse.db.VedtakBegrunnelseDao
import no.nav.helse.db.VedtakBegrunnelseFraDatabase
import no.nav.helse.db.VedtakBegrunnelseMedSaksbehandlerIdentFraDatabase
import no.nav.helse.db.VedtakBegrunnelseTypeFraDatabase
import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.modell.vedtak.VedtakBegrunnelse
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.QueryRunner
import java.util.UUID
import javax.sql.DataSource

class PgVedtakBegrunnelseDao internal constructor(
    queryRunner: QueryRunner,
) : QueryRunner by queryRunner,
    VedtakBegrunnelseDao {
    internal constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    private fun lagreBegrunnelse(
        vedtakBegrunnelse: VedtakBegrunnelseFraDatabase,
        saksbehandlerOid: UUID,
    ) = asSQL(
        """
        INSERT INTO begrunnelse(tekst, type, saksbehandler_ref) VALUES (:tekst, :type, :saksbehandler_ref)
        """.trimIndent(),
        "tekst" to vedtakBegrunnelse.tekst,
        "type" to vedtakBegrunnelse.type.name,
        "saksbehandler_ref" to saksbehandlerOid,
    ).updateAndReturnGeneratedKey()

    override fun lagreVedtakBegrunnelse(
        oppgaveId: Long,
        vedtakBegrunnelse: VedtakBegrunnelseFraDatabase,
        saksbehandlerOid: UUID,
    ) = lagreBegrunnelse(vedtakBegrunnelse, saksbehandlerOid).let { begrunnelseId ->
        asSQL(
            """
            INSERT INTO vedtak_begrunnelse (vedtaksperiode_id, begrunnelse_ref, behandling_ref)
            SELECT v.vedtaksperiode_id, :begrunnelseId, b.id
            FROM vedtaksperiode v
            INNER JOIN oppgave o on v.id = o.vedtak_ref
            INNER JOIN behandling b ON b.unik_id = o.generasjon_ref
            WHERE o.id = :oppgaveId
            """.trimIndent(),
            "oppgaveId" to oppgaveId,
            "begrunnelseId" to begrunnelseId,
        ).update().also { affectedRows ->
            check(affectedRows == 1) { "Insert av vedtaksbegrunnelse feilet ($affectedRows rader ble insertet)" }
        }
    }

    override fun invaliderVedtakBegrunnelse(oppgaveId: Long) =
        asSQL(
            """
            WITH t as ( 
                SELECT v.vedtaksperiode_id, b.id 
                FROM vedtaksperiode v
                INNER JOIN oppgave o ON v.id = o.vedtak_ref
                INNER JOIN behandling b ON o.generasjon_ref = b.unik_id
                WHERE o.id = :oppgaveId
            )
            UPDATE vedtak_begrunnelse vb
            SET invalidert = true
            FROM t
            WHERE vb.vedtaksperiode_id = t.vedtaksperiode_id AND vb.behandling_ref = t.id
            """.trimIndent(),
            "oppgaveId" to oppgaveId,
        ).update()

    override fun finnVedtakBegrunnelse(
        vedtaksperiodeId: UUID,
        generasjonId: Long,
    ): VedtakBegrunnelse? =
        asSQL(
            """
            SELECT b.type, b.tekst FROM vedtak_begrunnelse AS vb, begrunnelse AS b
            WHERE vb.vedtaksperiode_id = :vedtaksperiodeId 
            AND vb.behandling_ref = :behandlingId 
            AND vb.invalidert = false 
            AND b.id = vb.begrunnelse_ref
            ORDER BY vb.opprettet DESC LIMIT 1
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
            "behandlingId" to generasjonId,
        ).singleOrNull { vedtakBegrunnelse ->
            val begrunnelse = vedtakBegrunnelse.string("tekst")
            val type = enumValueOf<VedtakBegrunnelseTypeFraDatabase>(vedtakBegrunnelse.string("type"))
            VedtakBegrunnelse(utfall = type.toDomain(), begrunnelse = begrunnelse)
        }

    override fun finnVedtakBegrunnelse(oppgaveId: Long): VedtakBegrunnelseFraDatabase? =
        asSQL(
            """
            SELECT b.type, b.tekst FROM begrunnelse AS b, vedtak_begrunnelse AS vb, oppgave AS o, behandling AS bh, vedtaksperiode AS v
            WHERE b.id = vb.begrunnelse_ref
            AND vb.invalidert = false 
            AND vb.vedtaksperiode_id = v.vedtaksperiode_id
            AND v.id = o.vedtak_ref
            AND vb.behandling_ref = bh.id 
            AND bh.unik_id = o.generasjon_ref
            AND o.id = :oppgaveId
            ORDER BY vb.opprettet DESC LIMIT 1
            """.trimIndent(),
            "oppgaveId" to oppgaveId,
        ).singleOrNull { begrunnelse ->
            val tekst = begrunnelse.string("tekst")
            val type = enumValueOf<VedtakBegrunnelseTypeFraDatabase>(begrunnelse.string("type"))
            VedtakBegrunnelseFraDatabase(type = type, tekst = tekst)
        }

    private fun VedtakBegrunnelseTypeFraDatabase.toDomain() =
        when (this) {
            VedtakBegrunnelseTypeFraDatabase.AVSLAG -> Utfall.AVSLAG
            VedtakBegrunnelseTypeFraDatabase.DELVIS_INNVILGELSE -> Utfall.DELVIS_INNVILGELSE
            VedtakBegrunnelseTypeFraDatabase.INNVILGELSE -> Utfall.INNVILGELSE
        }

    override fun finnAlleVedtakBegrunnelser(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ) = asSQL(
        """
        SELECT b.type, b.tekst, vb.opprettet, s.ident, vb.invalidert FROM vedtak_begrunnelse vb
        INNER JOIN behandling beh ON vb.behandling_ref = beh.id 
        INNER JOIN begrunnelse b ON b.id = vb.begrunnelse_ref
        INNER JOIN saksbehandler s ON s.oid = b.saksbehandler_ref
        WHERE vb.vedtaksperiode_id = :vedtaksperiodeId AND beh.utbetaling_id = :utbetalingId 
        ORDER BY opprettet DESC
        """.trimIndent(),
        "vedtaksperiodeId" to vedtaksperiodeId,
        "utbetalingId" to utbetalingId,
    ).list { vedtakBegrunnelse ->
        VedtakBegrunnelseMedSaksbehandlerIdentFraDatabase(
            type = enumValueOf(vedtakBegrunnelse.string("type")),
            begrunnelse = vedtakBegrunnelse.string("tekst"),
            opprettet = vedtakBegrunnelse.localDateTime("opprettet"),
            saksbehandlerIdent = vedtakBegrunnelse.string("ident"),
            invalidert = vedtakBegrunnelse.boolean("invalidert"),
        )
    }
}
