package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.db.OverstyringDao
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import java.util.UUID
import javax.sql.DataSource

class PgOverstyringDao private constructor(queryRunner: QueryRunner) : OverstyringDao, QueryRunner by queryRunner {
    internal constructor(session: Session) : this(MedSession(session))
    internal constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    override fun kobleOverstyringOgVedtaksperiode(
        vedtaksperiodeIder: List<UUID>,
        overstyringHendelseId: UUID,
    ) {
        vedtaksperiodeIder.forEach { vedtaksperiode ->
            asSQL(
                """
                INSERT INTO overstyringer_for_vedtaksperioder (vedtaksperiode_id, overstyring_ref)
                SELECT :vedtaksperiodeId, o.id
                FROM overstyring o
                WHERE o.ekstern_hendelse_id = :overstyringHendelseId
                ON CONFLICT DO NOTHING
                """.trimIndent(),
                "vedtaksperiodeId" to vedtaksperiode,
                "overstyringHendelseId" to overstyringHendelseId,
            ).update()
        }
    }

    override fun harVedtaksperiodePågåendeOverstyring(vedtaksperiodeId: UUID) =
        asSQL(
            """
            SELECT 1
            FROM overstyringer_for_vedtaksperioder ofv
            JOIN overstyring o ON o.id = ofv.overstyring_ref
            WHERE ofv.vedtaksperiode_id = :vedtaksperiodeId
              AND o.ferdigstilt = false
            LIMIT 1
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).singleOrNull { true } ?: false

    override fun finnesEksternHendelseId(eksternHendelseId: UUID) =
        asSQL(
            """
            SELECT 1 from overstyring
            WHERE ekstern_hendelse_id = :eksternHendelseId
            """.trimIndent(),
            "eksternHendelseId" to eksternHendelseId,
        ).singleOrNull { true } ?: false
}
