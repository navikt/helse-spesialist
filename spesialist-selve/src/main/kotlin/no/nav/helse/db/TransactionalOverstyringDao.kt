package no.nav.helse.db

import kotliquery.Session
import kotliquery.queryOf
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import org.intellij.lang.annotations.Language
import java.util.UUID
import javax.naming.OperationNotSupportedException

class TransactionalOverstyringDao(
    private val session: Session,
) : OverstyringRepository {
    override fun finnOverstyringerMedTypeForVedtaksperioder(vedtaksperiodeIder: List<UUID>): List<OverstyringType> {
        throw OperationNotSupportedException()
    }

    override fun finnOverstyringerMedTypeForVedtaksperiode(vedtaksperiodeId: UUID): List<OverstyringType> {
        throw OperationNotSupportedException()
    }

    override fun finnesEksternHendelseId(eksternHendelseId: UUID): Boolean {
        @Language("PostgreSQL")
        val statement = """
            SELECT 1 from overstyring 
            WHERE ekstern_hendelse_id = :eksternHendelseId
        """
        return session.run(
            queryOf(
                statement,
                mapOf("eksternHendelseId" to eksternHendelseId),
            ).map { row -> row.boolean(1) }.asSingle,
        ) ?: false
    }

    override fun kobleOverstyringOgVedtaksperiode(
        vedtaksperiodeIder: List<UUID>,
        overstyringHendelseId: UUID,
    ) {
        @Language("PostgreSQL")
        val statement =
            """
            INSERT INTO overstyringer_for_vedtaksperioder(vedtaksperiode_id, overstyring_ref)
            SELECT :vedtaksperiode_id, o.id
            FROM overstyring o
            WHERE o.ekstern_hendelse_id = :overstyring_hendelse_id
            ON CONFLICT DO NOTHING
            """.trimIndent()
        vedtaksperiodeIder.forEach { vedtaksperiode ->
            session.run(
                queryOf(
                    statement,
                    mapOf(
                        "vedtaksperiode_id" to vedtaksperiode,
                        "overstyring_hendelse_id" to overstyringHendelseId,
                    ),
                ).asUpdate,
            )
        }
    }

    override fun harVedtaksperiodePågåendeOverstyring(vedtaksperiodeId: UUID): Boolean {
        @Language("PostgreSQL")
        val statement =
            """
            SELECT 1 FROM overstyringer_for_vedtaksperioder ofv
            JOIN overstyring o ON o.id = ofv.overstyring_ref
            WHERE ofv.vedtaksperiode_id = :vedtaksperiode_id
            AND o.ferdigstilt = false
            LIMIT 1
            """.trimIndent()
        return session.run(
            queryOf(statement, mapOf("vedtaksperiode_id" to vedtaksperiodeId))
                .map { row -> row.boolean(1) }.asSingle,
        ) ?: false
    }
}
