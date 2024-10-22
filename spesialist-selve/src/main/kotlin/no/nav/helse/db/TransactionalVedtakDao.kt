package no.nav.helse.db

import kotliquery.Session
import kotliquery.queryOf
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.HelseDao.Companion.single
import no.nav.helse.HelseDao.Companion.update
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import org.intellij.lang.annotations.Language
import java.util.UUID

class TransactionalVedtakDao(private val session: Session) : VedtakRepository {
    override fun leggTilVedtaksperiodetype(
        vedtaksperiodeId: UUID,
        type: Periodetype,
        inntektskilde: Inntektskilde,
    ) {
        val vedtakRef = finnVedtakId(vedtaksperiodeId) ?: return
        asSQL(
            """
            INSERT INTO saksbehandleroppgavetype (type, inntektskilde, vedtak_ref) VALUES (:type, :inntektskilde, :vedtak_ref)
            ON CONFLICT (vedtak_ref) DO UPDATE SET type = :type, inntektskilde = :inntektskilde
            """.trimIndent(),
            "type" to type.name,
            "inntektskilde" to inntektskilde.name,
            "vedtak_ref" to vedtakRef,
        ).update(session)
    }

    internal fun finnVedtakId(vedtaksperiodeId: UUID) =
        asSQL("SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId", "vedtaksperiodeId" to vedtaksperiodeId).single(session) {
            it.long("id")
        }

    override fun erSpesialsak(vedtaksperiodeId: UUID): Boolean {
        @Language("PostgreSQL")
        val query =
            """SELECT true FROM spesialsak WHERE vedtaksperiode_id = :vedtaksperiode_id AND ferdigbehandlet = false"""
        return session.run(
            queryOf(query, mapOf("vedtaksperiode_id" to vedtaksperiodeId))
                .map { it.boolean(1) }.asSingle,
        ) ?: false
    }

    override fun erAutomatiskGodkjent(utbetalingId: UUID) =
        asSQL(
            """
            SELECT automatisert FROM automatisering 
            WHERE utbetaling_id = :utbetalingId
            AND (inaktiv_fra IS NULL OR inaktiv_fra > now())
            ORDER BY id DESC
            LIMIT 1
            """.trimIndent(),
            "utbetalingId" to utbetalingId,
        ).single(session) {
            it.boolean("automatisert")
        } ?: false

    override fun opprettKobling(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) {
        @Language("PostgreSQL")
        val statement =
            """
            INSERT INTO vedtaksperiode_hendelse(vedtaksperiode_id, hendelse_ref) VALUES (?, ?)
            ON CONFLICT DO NOTHING
            """.trimIndent()
        session.run(queryOf(statement, vedtaksperiodeId, hendelseId).asUpdate)
    }

    override fun fjernKobling(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) {
        @Language("PostgreSQL")
        val statement = "DELETE FROM vedtaksperiode_hendelse WHERE hendelse_ref = ? AND vedtaksperiode_id = ?"
        session.run(queryOf(statement, hendelseId, vedtaksperiodeId).asUpdate)
    }

    override fun finnOrgnummer(vedtaksperiodeId: UUID): String? {
        @Language("PostgreSQL")
        val query = """
            SELECT orgnummer FROM arbeidsgiver a
            INNER JOIN vedtak v ON a.id = v.arbeidsgiver_ref
            WHERE v.vedtaksperiode_id = :vedtaksperiodeId
        """
        return session.run(
            queryOf(
                query,
                mapOf("vedtaksperiodeId" to vedtaksperiodeId),
            ).map { it.long("orgnummer").toString() }.asSingle,
        )
    }

    override fun finnInntektskilde(vedtaksperiodeId: UUID): Inntektskilde? {
        @Language("PostgreSQL")
        val statement =
            "SELECT inntektskilde FROM saksbehandleroppgavetype WHERE vedtak_ref = (SELECT id FROM vedtak WHERE vedtaksperiode_id = :vedtaksperiodeId)"
        return session.run(
            queryOf(statement, mapOf("vedtaksperiodeId" to vedtaksperiodeId)).map {
                enumValueOf<Inntektskilde>(it.string("inntektskilde"))
            }.asSingle,
        )
    }
}
