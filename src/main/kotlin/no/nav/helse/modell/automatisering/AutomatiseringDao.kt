package no.nav.helse.modell.automatisering

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
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

    private fun lagre(automatisert: Boolean, stikkprøve: Boolean, vedtaksperiodeId: UUID, hendelseId: UUID, problems: List<String> = emptyList(), utbetalingId: UUID) {
        sessionOf(dataSource).use { session ->
            session.transaction { transactionalSession ->
                transactionalSession.run(
                    queryOf(
                        """
                            INSERT INTO automatisering (vedtaksperiode_ref, hendelse_ref, automatisert, stikkprøve, utbetaling_id)
                            VALUES ((SELECT id FROM vedtak WHERE vedtaksperiode_id = ?), ?, ?, ?, ?)
                        """,
                        vedtaksperiodeId,
                        hendelseId,
                        automatisert,
                        stikkprøve,
                        utbetalingId
                    ).asUpdate
                )

                problems.forEach { problem ->
                    transactionalSession.run(
                        queryOf(
                            "INSERT INTO automatisering_problem(vedtaksperiode_ref, hendelse_ref, problem) VALUES ((SELECT id FROM vedtak WHERE vedtaksperiode_id = ?), ?, ?)",
                            vedtaksperiodeId, hendelseId, problem
                        ).asUpdate
                    )
                }
            }
        }
    }

    fun hentAutomatisering(vedtaksperiodeId: UUID, hendelseId: UUID) =
        sessionOf(dataSource).use { session ->
            val vedtaksperiodeRef = finnVedtaksperiode(vedtaksperiodeId) ?: return null

            @Language("PostgreSQL")
            val query =
                """
                SELECT a.automatisert automatisert, v.vedtaksperiode_id vedtaksperiode_id, h.id hendelse_id, a.utbetaling_id utbetaling_id
                FROM automatisering a
                         JOIN vedtak v ON a.vedtaksperiode_ref = v.id
                         JOIN hendelse h on h.id = a.hendelse_ref
                WHERE vedtaksperiode_ref = ? AND hendelse_ref = ?
                """
            session.run(
                queryOf(
                    query,
                    vedtaksperiodeRef,
                    hendelseId
                ).map { row ->
                    AutomatiseringDto(
                        automatisert = row.boolean("automatisert"),
                        vedtaksperiodeId = UUID.fromString(row.string("vedtaksperiode_id")),
                        hendelseId = UUID.fromString(row.string("hendelse_id")),
                        problemer = session.run(
                            queryOf(
                                "SELECT * FROM automatisering_problem WHERE vedtaksperiode_ref = ? AND hendelse_ref = ?",
                                vedtaksperiodeRef,
                                hendelseId
                            ).map { it.string("problem") }.asList
                        ),
                        utbetalingId = row.stringOrNull("utbetaling_id")?.let(UUID::fromString)
                    )
                }.asSingle
            )
        }

    fun hentAutomatisering(utbetalingId: UUID) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                """
                SELECT a.automatisert automatisert, v.vedtaksperiode_id vedtaksperiode_id, v.id vedtaksperiode_ref, h.id hendelse_id, a.utbetaling_id utbetaling_id
                FROM automatisering a
                         JOIN vedtak v ON a.vedtaksperiode_ref = v.id
                         JOIN hendelse h on h.id = a.hendelse_ref
                WHERE a.utbetaling_id = ?
                """
            session.run(
                queryOf(
                    query,
                    utbetalingId,
                ).map { row ->
                    val vedtaksperiodeId = UUID.fromString(row.string("vedtaksperiode_id"))
                    val vedtaksperiodeRef = row.long("vedtaksperiode_ref")
                    val hendelseId = UUID.fromString(row.string("hendelse_id"))
                    AutomatiseringDto(
                        automatisert = row.boolean("automatisert"),
                        vedtaksperiodeId = vedtaksperiodeId,
                        hendelseId = hendelseId,
                        problemer = session.run(
                            queryOf(
                                "SELECT * FROM automatisering_problem WHERE vedtaksperiode_ref = ? AND hendelse_ref = ?",
                                vedtaksperiodeRef,
                                hendelseId
                            ).map { it.string("problem") }.asList
                        ),
                        utbetalingId = row.stringOrNull("utbetaling_id")?.let(UUID::fromString)
                    )
                }.asSingle
            )
        }

    fun plukketUtTilStikkprøve(vedtaksperiodeId: UUID, hendelseId: UUID) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                """
                    SELECT a.stikkprøve FROM automatisering a
                    WHERE vedtaksperiode_ref = ( SELECT id FROM vedtak WHERE vedtak.vedtaksperiode_id = ? LIMIT 1)
                    AND hendelse_ref = ?
                """
            session.run(
                queryOf(query, vedtaksperiodeId, hendelseId)
                    .map { it.boolean(1) }.asSingle
            )
        } ?: false

    private fun finnVedtaksperiode(vedtaksperiodeId: UUID): Long? = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "SELECT id FROM vedtak WHERE vedtaksperiode_id = ?",
                vedtaksperiodeId
            ).map { it.long(1) }.asSingle
        )
    }
}

data class AutomatiseringDto(
    val automatisert: Boolean,
    val vedtaksperiodeId: UUID,
    val hendelseId: UUID,
    val problemer: List<String>,
    val utbetalingId: UUID?
)
