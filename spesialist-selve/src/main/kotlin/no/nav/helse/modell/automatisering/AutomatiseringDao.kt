package no.nav.helse.modell.automatisering

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.HelseDao.Companion.list
import no.nav.helse.HelseDao.Companion.single
import no.nav.helse.HelseDao.Companion.update
import no.nav.helse.db.AutomatiseringRepository
import java.util.UUID

class AutomatiseringDao(val session: Session) : AutomatiseringRepository {
    override fun settAutomatiseringInaktiv(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) {
        asSQL(
            """
            UPDATE automatisering
            SET inaktiv_fra = now()
            WHERE vedtaksperiode_ref = (
                SELECT id FROM vedtak WHERE vedtak.vedtaksperiode_id = :vedtaksperiodeId LIMIT 1
            )
            AND hendelse_ref = :hendelseRef
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
            "hendelseRef" to hendelseId,
        ).update(session)
    }

    override fun settAutomatiseringProblemInaktiv(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) {
        asSQL(
            """
            UPDATE automatisering_problem
            SET inaktiv_fra = now()
            WHERE vedtaksperiode_ref = (
                SELECT id FROM vedtak WHERE vedtak.vedtaksperiode_id = :vedtaksperiodeId LIMIT 1
            )
            AND hendelse_ref = :hendelseRef
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
            "hendelseRef" to hendelseId,
        ).update(session)
    }

    override fun plukketUtTilStikkprøve(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) = asSQL(
        """
        SELECT a.stikkprøve FROM automatisering a
        WHERE vedtaksperiode_ref = (
            SELECT id FROM vedtak WHERE vedtak.vedtaksperiode_id = :vedtaksperiodeId LIMIT 1
        )
        AND hendelse_ref = :hendelseId
        AND (inaktiv_fra IS NULL OR inaktiv_fra > now())
        """.trimIndent(),
        "vedtaksperiodeId" to vedtaksperiodeId, "hendelseId" to hendelseId,
    ).single(session) { it.boolean(1) } ?: false

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
        val vedtaksperiodeRef = finnVedtaksperiode(vedtaksperiodeId) ?: return null
        val problemer = finnAktiveProblemer(vedtaksperiodeRef, hendelseId)
        return asSQL(
            """
            SELECT a.automatisert, v.vedtaksperiode_id, h.id hendelse_id, a.utbetaling_id
            FROM automatisering a
                JOIN vedtak v ON a.vedtaksperiode_ref = v.id
                JOIN hendelse h ON h.id = a.hendelse_ref
            WHERE vedtaksperiode_ref = :vedtaksperiodeRef
            AND hendelse_ref = :hendelseId
            AND (inaktiv_fra IS NULL)
            """.trimIndent(),
            "vedtaksperiodeRef" to vedtaksperiodeRef,
            "hendelseId" to hendelseId,
        ).single(session) { tilAutomatiseringDto(problemer, it) }
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

        asSQL(
            """
            INSERT INTO automatisering (vedtaksperiode_ref, hendelse_ref, automatisert, stikkprøve, utbetaling_id)
            VALUES ((SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId), :hendelseId, :automatisert, :stikkproeve, :utbetalingId)
            """.trimIndent(),
            "vedtaksperiodeId" to vedtaksperiodeId,
            "hendelseId" to hendelseId,
            "automatisert" to automatisert,
            "stikkproeve" to stikkprøve,
            "utbetalingId" to utbetalingId,
        ).update(session)

        problems.forEach { problem ->
            asSQL(
                """
                INSERT INTO automatisering_problem(vedtaksperiode_ref, hendelse_ref, problem)
                VALUES ((SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId), :hendelseId, :problem)
                """.trimIndent(),
                "vedtaksperiodeId" to vedtaksperiodeId,
                "hendelseId" to hendelseId,
                "problem" to problem,
            ).update(session)
        }
    }

    override fun finnAktiveProblemer(
        vedtaksperiodeRef: Long,
        hendelseId: UUID,
    ) = asSQL(
        """
        SELECT * FROM automatisering_problem
        WHERE hendelse_ref = :hendelseId
        AND vedtaksperiode_ref = :vedtaksperiodeRef
        AND (inaktiv_fra IS NULL OR inaktiv_fra > now())
        """.trimIndent(),
        "hendelseId" to hendelseId,
        "vedtaksperiodeRef" to vedtaksperiodeRef,
    ).list(session) { it.string("problem") }

    override fun finnVedtaksperiode(vedtaksperiodeId: UUID) =
        asSQL(
            "SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId",
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).single(session) { it.long(1) }

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

data class AutomatiseringDto(
    val automatisert: Boolean,
    val vedtaksperiodeId: UUID,
    val hendelseId: UUID,
    val problemer: List<String>,
    val utbetalingId: UUID?,
)
