package no.nav.helse.db

import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.modell.vedtak.VedtakBegrunnelseDto
import no.nav.helse.modell.vedtak.VedtakBegrunnelseDto.UtfallDto
import java.util.UUID
import javax.sql.DataSource

class VedtakBegrunnelseDao(queryRunner: QueryRunner) : QueryRunner by queryRunner {
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

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

    internal fun lagreVedtakBegrunnelse(
        oppgaveId: Long,
        type: VedtakBegrunnelseTypeFraDatabase,
        begrunnelse: String,
        saksbehandlerOid: UUID,
    ) = lagreVedtakBegrunnelse(oppgaveId, VedtakBegrunnelseFraDatabase(type, begrunnelse), saksbehandlerOid)

    internal fun lagreVedtakBegrunnelse(
        oppgaveId: Long,
        vedtakBegrunnelse: VedtakBegrunnelseFraDatabase,
        saksbehandlerOid: UUID,
    ) = lagreBegrunnelse(vedtakBegrunnelse, saksbehandlerOid).let { begrunnelseId ->
        asSQL(
            """
            INSERT INTO vedtak_begrunnelse (vedtaksperiode_id, begrunnelse_ref, generasjon_ref)
            SELECT v.vedtaksperiode_id, :begrunnelseId, b.id
            FROM vedtak v
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

    internal fun invaliderVedtakBegrunnelse(oppgaveId: Long) =
        asSQL(
            """
            WITH t as ( 
                SELECT v.vedtaksperiode_id, b.id 
                FROM vedtak v
                INNER JOIN oppgave o ON v.id = o.vedtak_ref
                INNER JOIN behandling b ON o.generasjon_ref = b.unik_id
                WHERE o.id = :oppgaveId
            )
            UPDATE vedtak_begrunnelse a
            SET invalidert = true
            FROM t
            WHERE a.vedtaksperiode_id = t.vedtaksperiode_id AND a.generasjon_ref = t.id
            """.trimIndent(),
            "oppgaveId" to oppgaveId,
        ).update()

    internal fun finnVedtakBegrunnelse(
        vedtaksperiodeId: UUID,
        generasjonId: Long,
    ): VedtakBegrunnelseDto? =
        asSQL(
            """
            SELECT b.type, b.tekst FROM vedtak_begrunnelse AS vb, begrunnelse AS b
            WHERE vb.vedtaksperiode_id = :vedtaksperiodeId 
            AND vb.generasjon_ref = :generasjonId 
            AND vb.invalidert = false 
            AND b.id = vb.begrunnelse_ref
            ORDER BY vb.opprettet DESC LIMIT 1
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
            "generasjonId" to generasjonId,
        ).singleOrNull { vedtakBegrunnelse ->
            val begrunnelse = vedtakBegrunnelse.string("tekst")
            val type = enumValueOf<VedtakBegrunnelseTypeFraDatabase>(vedtakBegrunnelse.string("type"))
            VedtakBegrunnelseDto(utfall = type.toDto(), begrunnelse = begrunnelse)
        }

    internal fun finnVedtakBegrunnelse(oppgaveId: Long): VedtakBegrunnelseFraDatabase? =
        asSQL(
            """
            SELECT b.type, b.tekst FROM begrunnelse AS b, vedtak_begrunnelse AS vb, oppgave AS o, behandling AS bh, vedtak AS v
            WHERE b.id = vb.begrunnelse_ref
            AND vb.invalidert = false 
            AND vb.vedtaksperiode_id = v.vedtaksperiode_id
            AND v.id = o.vedtak_ref
            AND vb.generasjon_ref = bh.id 
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

    private fun VedtakBegrunnelseTypeFraDatabase.toDto() =
        when (this) {
            VedtakBegrunnelseTypeFraDatabase.AVSLAG -> UtfallDto.AVSLAG
            VedtakBegrunnelseTypeFraDatabase.DELVIS_INNVILGELSE -> UtfallDto.DELVIS_INNVILGELSE
            VedtakBegrunnelseTypeFraDatabase.INNVILGELSE -> UtfallDto.INNVILGELSE
        }

    internal fun finnAlleVedtakBegrunnelser(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ) = asSQL(
        """
        SELECT b.type, b.tekst, a.opprettet, s.ident, a.invalidert FROM vedtak_begrunnelse a 
        INNER JOIN behandling beh ON a.generasjon_ref = beh.id 
        INNER JOIN begrunnelse b ON b.id = a.begrunnelse_ref
        INNER JOIN saksbehandler s ON s.oid = b.saksbehandler_ref
        WHERE a.vedtaksperiode_id = :vedtaksperiodeId AND beh.utbetaling_id = :utbetalingId 
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
