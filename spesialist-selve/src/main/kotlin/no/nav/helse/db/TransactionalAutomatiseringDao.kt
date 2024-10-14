package no.nav.helse.db

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.helse.modell.automatisering.AutomatiseringDto
import org.intellij.lang.annotations.Language
import java.util.UUID

class TransactionalAutomatiseringDao(private val transactionalSession: TransactionalSession) :
    AutomatiseringRepository {
    override fun settAutomatiseringInaktiv(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) {
        @Language("PostgreSQL")
        val query =
            """
             UPDATE automatisering
            SET inaktiv_fra = now()
            WHERE vedtaksperiode_ref = (SELECT id FROM vedtak WHERE vedtak.vedtaksperiode_id = :vedtaksperiode_id LIMIT 1)
            AND hendelse_ref = :hendelse_ref
            """.trimIndent()
        transactionalSession.run(
            queryOf(
                query,
                mapOf(
                    "vedtaksperiode_id" to vedtaksperiodeId,
                    "hendelse_ref" to hendelseId,
                ),
            ).asUpdate,
        )
    }

    override fun settAutomatiseringProblemInaktiv(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) {
        @Language("PostgreSQL")
        val query =
            """
            UPDATE automatisering_problem
            SET inaktiv_fra = now()
            WHERE vedtaksperiode_ref = (SELECT id FROM vedtak WHERE vedtak.vedtaksperiode_id = :vedtaksperiode_id LIMIT 1)
            AND hendelse_ref = :hendelse_ref
            """.trimIndent()
        transactionalSession.run(
            queryOf(
                query,
                mapOf(
                    "vedtaksperiode_id" to vedtaksperiodeId,
                    "hendelse_ref" to hendelseId,
                ),
            ).asUpdate,
        )
    }

    override fun plukketUtTilStikkprøve(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ): Boolean {
        @Language("PostgreSQL")
        val query =
            """
            SELECT a.stikkprøve FROM automatisering a
            WHERE vedtaksperiode_ref = ( SELECT id FROM vedtak WHERE vedtak.vedtaksperiode_id = ? LIMIT 1)
            AND hendelse_ref = ?
            AND (inaktiv_fra IS NULL OR inaktiv_fra > now())
            """.trimIndent()
        return transactionalSession.run(
            queryOf(query, vedtaksperiodeId, hendelseId)
                .map { it.boolean(1) }.asSingle,
        ) ?: false
    }

    override fun automatisert(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        utbetalingId: UUID,
    ) = lagre(automatisert = true, stikkprøve = false, vedtaksperiodeId, hendelseId, utbetalingId = utbetalingId)

    override fun manuellSaksbehandling(
        problems: List<String>,
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        utbetalingId: UUID,
    ) = lagre(automatisert = false, stikkprøve = false, vedtaksperiodeId, hendelseId, problems, utbetalingId)

    override fun stikkprøve(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        utbetalingId: UUID,
    ) = lagre(automatisert = false, stikkprøve = true, vedtaksperiodeId, hendelseId, utbetalingId = utbetalingId)

    override fun hentAktivAutomatisering(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ): AutomatiseringDto? {
        @Language("PostgreSQL")
        val statement =
            """
            SELECT a.automatisert, v.vedtaksperiode_id, h.id hendelse_id, a.utbetaling_id
            FROM automatisering a
                JOIN vedtak v ON a.vedtaksperiode_ref = v.id
                JOIN hendelse h ON h.id = a.hendelse_ref
            WHERE vedtaksperiode_ref = ? 
            AND hendelse_ref = ?
            AND (inaktiv_fra IS NULL)
            """.trimIndent()
        val vedtaksperiodeRef = finnVedtaksperiode(vedtaksperiodeId) ?: return null
        val problemer = finnAktiveProblemer(vedtaksperiodeRef, hendelseId)
        return transactionalSession.run(
            queryOf(statement, vedtaksperiodeRef, hendelseId)
                .map { tilAutomatiseringDto(problemer, it) }
                .asSingle,
        )
    }

    private fun lagre(
        automatisert: Boolean,
        stikkprøve: Boolean,
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
        problems: List<String> = emptyList(),
        utbetalingId: UUID,
    ) {
        hentAktivAutomatisering(vedtaksperiodeId, hendelseId)?.also {
            throw Exception(
                "Det kan bare finnes 1 aktiv automatisering. Klarer ikke opprette ny automatisering for vedtaksperiodeId $vedtaksperiodeId og hendelseId $hendelseId.",
            )
        }

        @Language("PostgreSQL")
        val insertAutomatiseringStatement =
            """
            INSERT INTO automatisering (vedtaksperiode_ref, hendelse_ref, automatisert, stikkprøve, utbetaling_id)
            VALUES ((SELECT id FROM vedtak WHERE vedtaksperiode_id = ?), ?, ?, ?, ?)
            """.trimIndent()
        transactionalSession.run(
            queryOf(
                insertAutomatiseringStatement,
                vedtaksperiodeId,
                hendelseId,
                automatisert,
                stikkprøve,
                utbetalingId,
            ).asUpdate,
        )

        @Language("PostgreSQL")
        val insertProblemStatement =
            """
            INSERT INTO automatisering_problem(vedtaksperiode_ref, hendelse_ref, problem)
            VALUES ((SELECT id FROM vedtak WHERE vedtaksperiode_id = ?), ?, ?)
            """.trimIndent()
        problems.forEach { problem ->
            transactionalSession.run(
                queryOf(
                    insertProblemStatement,
                    vedtaksperiodeId,
                    hendelseId,
                    problem,
                ).asUpdate,
            )
        }
    }

    override fun finnAktiveProblemer(
        vedtaksperiodeRef: Long,
        hendelseId: UUID,
    ): List<String> {
        @Language("PostgreSQL")
        val statement =
            """
            SELECT * FROM automatisering_problem 
            WHERE hendelse_ref = ? 
            AND vedtaksperiode_ref = ?
            AND (inaktiv_fra IS NULL OR inaktiv_fra > now())
            """.trimIndent()
        return transactionalSession.run(
            queryOf(statement, hendelseId, vedtaksperiodeRef)
                .map { it.string("problem") }
                .asList,
        )
    }

    override fun finnVedtaksperiode(vedtaksperiodeId: UUID): Long? {
        @Language("PostgreSQL")
        val statement = "SELECT id FROM vedtak WHERE vedtaksperiode_id = ?"
        return transactionalSession.run(
            queryOf(statement, vedtaksperiodeId)
                .map { it.long(1) }
                .asSingle,
        )
    }

    private fun tilAutomatiseringDto(
        problemer: List<String>,
        row: Row,
    ) = AutomatiseringDto(
        automatisert = row.boolean("automatisert"),
        vedtaksperiodeId = row.uuid("vedtaksperiode_id"),
        hendelseId = row.uuid("hendelse_id"),
        problemer = problemer,
        utbetalingId = row.stringOrNull("utbetaling_id")?.let(UUID::fromString),
    )
}
