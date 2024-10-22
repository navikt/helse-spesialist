package no.nav.helse.db

import kotliquery.Session
import kotliquery.queryOf
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.HelseDao.Companion.asSQLForQuestionMarks
import no.nav.helse.HelseDao.Companion.list
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import org.intellij.lang.annotations.Language
import java.util.UUID

class TransactionalOverstyringDao(
    private val session: Session,
) : OverstyringRepository {
    override fun finnOverstyringerMedTypeForVedtaksperioder(vedtaksperiodeIder: List<UUID>) =
        asSQLForQuestionMarks(
            """
            SELECT DISTINCT o.id,
                CASE
                    WHEN oi.id IS NOT NULL THEN 'Inntekt'
                    WHEN oa.id IS NOT NULL THEN 'Arbeidsforhold'
                    WHEN ot.id IS NOT NULL THEN 'Dager'
                    WHEN ss.id IS NOT NULL THEN 'Sykepengegrunnlag'
                    WHEN oms.id IS NOT NULL THEN 'MinimumSykdomsgrad'
                END as type
            FROM overstyring o
            LEFT JOIN overstyring_arbeidsforhold oa on o.id = oa.overstyring_ref
            LEFT JOIN overstyring_inntekt oi on o.id = oi.overstyring_ref
            LEFT JOIN overstyring_tidslinje ot on o.id = ot.overstyring_ref
            LEFT JOIN skjonnsfastsetting_sykepengegrunnlag ss on o.id = ss.overstyring_ref
            LEFT JOIN overstyring_minimum_sykdomsgrad oms on o.id = oms.overstyring_ref
            WHERE o.vedtaksperiode_id IN (${vedtaksperiodeIder.joinToString { "?" }})
            AND o.ferdigstilt = false
            """.trimIndent(),
            *vedtaksperiodeIder.toTypedArray(),
        ).list(session) { OverstyringType.valueOf(it.string("type")) }

    override fun finnOverstyringerMedTypeForVedtaksperiode(vedtaksperiodeId: UUID): List<OverstyringType> {
        return asSQL(
            """
            SELECT DISTINCT o.id,
                CASE
                    WHEN oi.id IS NOT NULL THEN 'Inntekt'
                    WHEN oa.id IS NOT NULL THEN 'Arbeidsforhold'
                    WHEN ot.id IS NOT NULL THEN 'Dager'
                    WHEN ss.id IS NOT NULL THEN 'Sykepengegrunnlag'
                    WHEN oms.id IS NOT NULL THEN 'MinimumSykdomsgrad'
                END as type
            FROM overstyring o
            LEFT JOIN overstyring_arbeidsforhold oa on o.id = oa.overstyring_ref
            LEFT JOIN overstyring_inntekt oi on o.id = oi.overstyring_ref
            LEFT JOIN overstyring_tidslinje ot on o.id = ot.overstyring_ref
            LEFT JOIN skjonnsfastsetting_sykepengegrunnlag ss on o.id = ss.overstyring_ref
            LEFT JOIN overstyring_minimum_sykdomsgrad oms on o.id = oms.overstyring_ref
            WHERE o.id IN (
                SELECT overstyring_ref FROM overstyringer_for_vedtaksperioder
                WHERE vedtaksperiode_id = :vedtaksperiodeId
            )
            AND o.ferdigstilt = false
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).list(session) { OverstyringType.valueOf(it.string("type")) }
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
