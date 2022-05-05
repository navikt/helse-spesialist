package no.nav.helse.modell.automatisering

import java.time.LocalDateTime
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

internal class AutomatiseringDao(val dataSource: DataSource) {
    internal fun manuellSaksbehandling(problems: List<String>, vedtaksperiodeId: UUID, hendelseId: UUID, utbetalingId: UUID) =
        lagre(automatisert = false, stikkprøve = false, vedtaksperiodeId, hendelseId, problems, utbetalingId)

    internal fun automatisert(vedtaksperiodeId: UUID, hendelseId: UUID, utbetalingId: UUID) =
        lagre(automatisert = true, stikkprøve = false, vedtaksperiodeId, hendelseId, utbetalingId = utbetalingId)

    internal fun stikkprøve(vedtaksperiodeId: UUID, hendelseId: UUID, utbetalingId: UUID) =
        lagre(automatisert = false, stikkprøve = true, vedtaksperiodeId, hendelseId, utbetalingId = utbetalingId)

    internal fun settAutomatiseringInaktiv(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        inaktiv_fra: LocalDateTime = LocalDateTime.now()
    ) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query =
            """ UPDATE automatisering
                SET inaktiv_fra = :inaktiv_fra
                WHERE vedtaksperiode_ref = (SELECT id FROM vedtak WHERE vedtak.vedtaksperiode_id = :vedtaksperiode_id LIMIT 1)
                AND hendelse_ref = :hendelse_ref
            """
        session.run(
            queryOf(
                query,
                mapOf(
                    "inaktiv_fra" to inaktiv_fra,
                    "vedtaksperiode_id" to vedtaksperiodeId,
                    "hendelse_ref" to hendelseId
                )
            ).asUpdate
        )
    }

    internal fun settAutomatiseringProblemInaktiv(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        inaktiv_fra: LocalDateTime = LocalDateTime.now()
    ) = sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                """ UPDATE automatisering_problem
                    SET inaktiv_fra = :inaktiv_fra
                    WHERE vedtaksperiode_ref = (SELECT id FROM vedtak WHERE vedtak.vedtaksperiode_id = :vedtaksperiode_id LIMIT 1)
                    AND hendelse_ref = :hendelse_ref
                """
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "inaktiv_fra" to inaktiv_fra,
                        "vedtaksperiode_id" to vedtaksperiodeId,
                        "hendelse_ref" to hendelseId
                    )
                ).asUpdate
            )
        }

    private fun lagre(automatisert: Boolean, stikkprøve: Boolean, vedtaksperiodeId: UUID, hendelseId: UUID, problems: List<String> = emptyList(), utbetalingId: UUID) {
        sessionOf(dataSource).use { session ->
            session.transaction { transactionalSession ->
                transactionalSession.run(
                    queryOf(
                        """
                            INSERT INTO automatisering (vedtaksperiode_ref, hendelse_ref, automatisert, stikkprøve, utbetaling_id)
                            VALUES ((SELECT id FROM vedtak WHERE vedtaksperiode_id = ?), ?, ?, ?, ?)
                        """,
                        vedtaksperiodeId, hendelseId, automatisert, stikkprøve, utbetalingId
                    ).asUpdate
                )

                problems.forEach { problem ->
                    transactionalSession.run(
                        queryOf(
                            """
                                INSERT INTO automatisering_problem(vedtaksperiode_ref, hendelse_ref, problem)
                                VALUES ((SELECT id FROM vedtak WHERE vedtaksperiode_id = ?), ?, ?)
                                """,
                            vedtaksperiodeId, hendelseId, problem
                        ).asUpdate
                    )
                }
            }
        }
    }

    internal fun plukketUtTilStikkprøve(vedtaksperiodeId: UUID, hendelseId: UUID) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                """
                    SELECT a.stikkprøve FROM automatisering a
                    WHERE vedtaksperiode_ref = ( SELECT id FROM vedtak WHERE vedtak.vedtaksperiode_id = ? LIMIT 1)
                    AND hendelse_ref = ?
                    AND (inaktiv_fra IS NULL OR inaktiv_fra > now())
                """
            session.run(
                queryOf(query, vedtaksperiodeId, hendelseId)
                    .map { it.boolean(1) }.asSingle
            )
        } ?: false

    internal fun hentAktivAutomatisering(vedtaksperiodeId: UUID, hendelseId: UUID) = sessionOf(dataSource).use { session ->
        val vedtaksperiodeRef = finnVedtaksperiode(vedtaksperiodeId) ?: return@use null
        val problemer = finnAktiveProblemer(vedtaksperiodeRef, hendelseId)

        @Language("PostgreSQL")
        val query = """
            SELECT a.automatisert automatisert, v.vedtaksperiode_id vedtaksperiode_id, h.id hendelse_id, a.utbetaling_id utbetaling_id
            FROM automatisering a
                JOIN vedtak v ON a.vedtaksperiode_ref = v.id
                JOIN hendelse h ON h.id = a.hendelse_ref
            WHERE vedtaksperiode_ref = ? 
            AND hendelse_ref = ?
            AND (inaktiv_fra IS NULL OR inaktiv_fra > now())
            """
        session.run(queryOf(query, vedtaksperiodeRef, hendelseId).map { row -> tilAutomatiseringDto(problemer, row) }.asSingle
        )
    }

    internal fun finnAktiveProblemer(vedtaksperiodeRef: Long, hendelseId: UUID) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = """
            SELECT * FROM automatisering_problem 
            WHERE hendelse_ref = ? 
            AND vedtaksperiode_ref = ?
            AND (inaktiv_fra IS NULL OR inaktiv_fra > now())
        """

        session.run(
            queryOf(query, hendelseId, vedtaksperiodeRef).map {
                it.string("problem")
            }.asList
        )
    }

    internal fun finnVedtaksperiode(vedtaksperiodeId: UUID): Long? = sessionOf(dataSource).use { session ->
        session.run(
            queryOf(
                """
                    SELECT id FROM vedtak 
                    WHERE vedtaksperiode_id = ?
                """.trimIndent(),
                vedtaksperiodeId
            ).map { it.long(1) }.asSingle
        )
    }

    private fun tilAutomatiseringDto(problemer: List<String>, row: Row) =
        AutomatiseringDto(
            automatisert = row.boolean("automatisert"),
            vedtaksperiodeId = UUID.fromString(row.string("vedtaksperiode_id")),
            hendelseId = UUID.fromString(row.string("hendelse_id")),
            problemer = problemer,
            utbetalingId = row.stringOrNull("utbetaling_id")?.let(UUID::fromString)
        )
}

data class AutomatiseringDto(
    val automatisert: Boolean,
    val vedtaksperiodeId: UUID,
    val hendelseId: UUID,
    val problemer: List<String>,
    val utbetalingId: UUID?
)
