package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveDao
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import java.util.UUID
import javax.sql.DataSource

class PgOppgaveDao internal constructor(
    queryRunner: QueryRunner,
) : OppgaveDao,
    QueryRunner by queryRunner {
    internal constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    internal constructor(session: Session) : this(MedSession(session))

    override fun finnBehandlingId(oppgaveId: Long): UUID =
        asSQL("SELECT spesialist_behandling_id FROM oppgave WHERE id = :oppgaveId", "oppgaveId" to oppgaveId)
            .single { it.uuid("spesialist_behandling_id") }

    override fun finnOppgaveIdUansettStatus(fødselsnummer: String): Long =
        asSQL(
            """
            SELECT o.id as oppgaveId
            FROM oppgave o
                     JOIN vedtaksperiode v ON v.id = o.vedtak_ref
                     JOIN person p ON v.person_ref = p.id
            WHERE p.fødselsnummer = :fodselsnummer
            ORDER BY o.id DESC
            LIMIT 1
            """,
            "fodselsnummer" to fødselsnummer,
        ).single {
            it.long("oppgaveId")
        }

    override fun finnOppgaveId(fødselsnummer: String): Long? =
        asSQL(
            """
            SELECT o.id as oppgaveId
            FROM oppgave o
            JOIN vedtaksperiode v ON v.id = o.vedtak_ref
            JOIN person p ON v.person_ref = p.id
            WHERE o.status = 'AvventerSaksbehandler'::oppgavestatus
                AND p.fødselsnummer = :fodselsnummer;
            """,
            "fodselsnummer" to fødselsnummer,
        ).singleOrNull {
            it.long("oppgaveId")
        }

    override fun finnOppgaveId(utbetalingId: UUID): Long? =
        asSQL(
            """
            SELECT o.id as oppgaveId
            FROM oppgave o WHERE o.utbetaling_id = :utbetaling_id
            AND o.status NOT IN ('Invalidert'::oppgavestatus, 'Ferdigstilt'::oppgavestatus)
            ORDER BY o.id DESC
            LIMIT 1
            """,
            "utbetaling_id" to utbetalingId,
        ).singleOrNull {
            it.long("oppgaveId")
        }

    override fun finnVedtaksperiodeId(oppgaveId: Long) =
        asSQL(
            """
            SELECT v.vedtaksperiode_id
            FROM vedtaksperiode v
            INNER JOIN oppgave o on v.id = o.vedtak_ref
            WHERE o.id = :oppgaveId
            """.trimIndent(),
            "oppgaveId" to oppgaveId,
        ).single { row -> row.uuid("vedtaksperiode_id") }

    override fun reserverNesteId(): Long =
        asSQL(
            """
            SELECT nextval(pg_get_serial_sequence('oppgave', 'id')) as neste_id 
            """,
        ).single {
            it.long("neste_id")
        }

    override fun finnSpleisBehandlingId(oppgaveId: Long): UUID =
        asSQL(
            """
            SELECT spleis_behandling_id FROM oppgave o
            INNER JOIN behandling b ON b.unik_id = o.spesialist_behandling_id
            WHERE o.id = :oppgaveId; 
            """,
            "oppgaveId" to oppgaveId,
        ).single {
            it.uuid("spleis_behandling_id")
        }

    override fun finnFødselsnummer(oppgaveId: Long): String =
        asSQL(
            """
            SELECT fødselsnummer from person
            INNER JOIN vedtaksperiode v on person.id = v.person_ref
            INNER JOIN oppgave o on v.id = o.vedtak_ref
            WHERE o.id = :oppgaveId
            """,
            "oppgaveId" to oppgaveId,
        ).single {
            it.string("fødselsnummer")
        }

    override fun harFerdigstiltOppgave(vedtaksperiodeId: UUID): Boolean =
        asSQL(
            """
            SELECT COUNT(1) AS oppgave_count FROM oppgave o
            INNER JOIN vedtaksperiode v on o.vedtak_ref = v.id
            WHERE v.vedtaksperiode_id = :vedtaksperiodeId AND o.status = 'Ferdigstilt'::oppgavestatus
            """,
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).single {
            it.int("oppgave_count")
        } > 0

    override fun finnEgenskaper(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
    ): Set<EgenskapForDatabase>? =
        asSQL(
            """
            SELECT o.egenskaper FROM oppgave o 
            INNER JOIN vedtaksperiode v ON o.vedtak_ref = v.id
            WHERE v.vedtaksperiode_id = :vedtaksperiodeId
            AND o.utbetaling_id = :utbetalingId
            ORDER BY o.opprettet DESC
            LIMIT 1 
            """,
            "vedtaksperiodeId" to vedtaksperiodeId,
            "utbetalingId" to utbetalingId,
        ).singleOrNull { row ->
            row.array<String>("egenskaper").map { enumValueOf<EgenskapForDatabase>(it) }.toSet()
        }

    override fun finnIdForAktivOppgave(vedtaksperiodeId: UUID): Long? =
        asSQL(
            """
            SELECT id FROM oppgave
            WHERE vedtak_ref =
                (SELECT id FROM vedtaksperiode WHERE vedtaksperiode_id = :vedtaksperiodeId)
                    AND status not in ('Ferdigstilt'::oppgavestatus, 'Invalidert'::oppgavestatus)
            ORDER BY opprettet DESC
            LIMIT 1
            """,
            "vedtaksperiodeId" to vedtaksperiodeId,
        ).singleOrNull {
            it.long("id")
        }
}
