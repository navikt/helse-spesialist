package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.db.VedtakDao
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import java.util.UUID
import javax.sql.DataSource

class PgVedtakDao private constructor(
    queryRunner: QueryRunner,
) : VedtakDao,
    QueryRunner by queryRunner {
    internal constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    internal constructor(session: Session) : this(MedSession(session))

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
        ).update()
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
        ).singleOrNull {
            it.boolean("automatisert")
        } ?: false

    override fun opprettKobling(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    ) {
        asSQL(
            """
            INSERT INTO vedtaksperiode_hendelse(vedtaksperiode_id, hendelse_ref) VALUES (:vedtaksperiodeId, :hendelseId)
            ON CONFLICT DO NOTHING
            """,
            "vedtaksperiodeId" to vedtaksperiodeId,
            "hendelseId" to hendelseId,
        ).update()
    }

    override fun finnInntektskilde(vedtaksperiodeId: UUID): Inntektskilde? =
        asSQL(
            "SELECT inntektskilde FROM saksbehandleroppgavetype WHERE vedtak_ref = (SELECT id FROM vedtaksperiode WHERE vedtaksperiode_id = :vedtaksperiodeId)",
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).singleOrNull {
            enumValueOf<Inntektskilde>(it.string("inntektskilde"))
        }

    fun finnVedtakId(vedtaksperiodeId: UUID): Long? =
        asSQL(
            "SELECT id FROM vedtaksperiode WHERE vedtaksperiode_id = :vedtaksperiodeId",
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).singleOrNull {
            it.long("id")
        }
}
