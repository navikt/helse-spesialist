package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.db.AntallOppgaverFraDatabase
import no.nav.helse.db.BehandletOppgaveFraDatabaseForVisning
import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.PersonnavnFraDatabase
import no.nav.helse.modell.gosysoppgaver.OppgaveDataForAutomatisering
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.db.objectMapper
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

class PgOppgaveDao internal constructor(
    queryRunner: QueryRunner,
) : OppgaveDao,
    QueryRunner by queryRunner {
    internal constructor(dataSource: DataSource) : this(MedDataSource(dataSource))
    internal constructor(session: Session) : this(MedSession(session))

    override fun finnGenerasjonId(oppgaveId: Long): UUID =
        asSQL("SELECT generasjon_ref FROM oppgave WHERE id = :oppgaveId", "oppgaveId" to oppgaveId)
            .single { it.uuid("generasjon_ref") }

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

    override fun invaliderOppgave(oppgaveId: Long) {
        asSQL(
            "UPDATE oppgave SET status = 'Invalidert' WHERE id = :id",
            "id" to oppgaveId,
        ).update()
    }

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
            INNER JOIN behandling b ON b.unik_id = o.generasjon_ref
            WHERE o.id = :oppgaveId; 
            """,
            "oppgaveId" to oppgaveId,
        ).single {
            it.uuid("spleis_behandling_id")
        }

    override fun oppgaveDataForAutomatisering(oppgaveId: Long): OppgaveDataForAutomatisering? =
        asSQL(
            """
            SELECT v.vedtaksperiode_id, v.fom, v.tom, o.utbetaling_id, h.id AS hendelseId, h.data AS godkjenningbehovJson, s.type as periodetype
            FROM vedtaksperiode v
            INNER JOIN oppgave o ON o.vedtak_ref = v.id
            INNER JOIN hendelse h ON h.id = o.hendelse_id_godkjenningsbehov
            INNER JOIN saksbehandleroppgavetype s ON s.vedtak_ref = v.id
            WHERE o.id = :oppgaveId
            """,
            "oppgaveId" to oppgaveId,
        ).singleOrNull { row ->
            val json = objectMapper.readTree(row.string("godkjenningbehovJson"))
            val skjæringstidspunkt =
                json
                    .path("Godkjenning")
                    .path("skjæringstidspunkt")
                    .asText()
                    .let(LocalDate::parse)
            OppgaveDataForAutomatisering(
                oppgaveId = oppgaveId,
                vedtaksperiodeId = row.uuid("vedtaksperiode_id"),
                periodeFom = row.localDate("fom"),
                periodeTom = row.localDate("tom"),
                skjæringstidspunkt = skjæringstidspunkt,
                utbetalingId = row.uuid("utbetaling_id"),
                hendelseId = row.uuid("hendelseId"),
                godkjenningsbehovJson = row.string("godkjenningbehovJson"),
                periodetype = enumValueOf(row.string("periodetype")),
            )
        }

    override fun finnAntallOppgaver(saksbehandlerOid: UUID): AntallOppgaverFraDatabase =
        asSQL(
            """
            SELECT
                count(*) FILTER ( WHERE NOT 'PÅ_VENT' = ANY (o.egenskaper) ) AS antall_mine_saker,
                count(*) FILTER ( WHERE 'PÅ_VENT' = ANY (o.egenskaper) ) AS antall_mine_saker_på_vent
            from oppgave o
                LEFT JOIN tildeling t ON o.id = t.oppgave_id_ref
            WHERE o.status = 'AvventerSaksbehandler'
                AND t.saksbehandler_ref = :oid 
            """,
            "oid" to saksbehandlerOid,
        ).singleOrNull { row ->
            AntallOppgaverFraDatabase(
                antallMineSaker = row.int("antall_mine_saker"),
                antallMineSakerPåVent = row.int("antall_mine_saker_på_vent"),
            )
        } ?: AntallOppgaverFraDatabase(antallMineSaker = 0, antallMineSakerPåVent = 0)

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

    override fun oppdaterPekerTilGodkjenningsbehov(
        godkjenningsbehovId: UUID,
        utbetalingId: UUID,
    ) {
        asSQL(
            """
            update oppgave
            set hendelse_id_godkjenningsbehov = :godkjenningsbehovId
            where utbetaling_id = :utbetalingId
            """.trimIndent(),
            "godkjenningsbehovId" to godkjenningsbehovId,
            "utbetalingId" to utbetalingId,
        ).update()
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

    override fun finnBehandledeOppgaver(
        behandletAvOid: UUID,
        offset: Int,
        limit: Int,
        fom: LocalDate,
        tom: LocalDate,
    ): List<BehandletOppgaveFraDatabaseForVisning> =
        asSQL(
            """
            SELECT
                o.id as oppgave_id,
                p.aktør_id,
                p.fødselsnummer,
                o.egenskaper,
                o.oppdatert as ferdigstilt_tidspunkt,
                o.ferdigstilt_av,
                ttv.beslutter,
                ttv.saksbehandler,
                pi.fornavn, pi.mellomnavn, pi.etternavn,
                count(1) OVER() AS filtered_count
            FROM oppgave o
                INNER JOIN vedtaksperiode v ON o.vedtak_ref = v.id
                INNER JOIN person p ON v.person_ref = p.id
                INNER JOIN person_info pi ON p.info_ref = pi.id
                LEFT JOIN (SELECT tv.person_ref, tv.tilstand, beslutter.ident as beslutter, saksbehandler.ident as saksbehandler
                         FROM totrinnsvurdering tv
                         INNER JOIN saksbehandler beslutter on tv.beslutter = beslutter.oid
                         INNER JOIN saksbehandler saksbehandler on tv.saksbehandler = saksbehandler.oid
                         WHERE (saksbehandler = :oid OR beslutter = :oid) AND (tv.oppdatert::date >= :fom::date AND tv.oppdatert::date <= :tom::date)
                     ) ttv ON ttv.person_ref = p.id
            WHERE (ttv.tilstand = 'GODKJENT' OR o.ferdigstilt_av_oid = :oid)
                AND (o.status in ('Ferdigstilt', 'AvventerSystem'))
                AND (o.oppdatert::date >= :fom::date AND o.oppdatert::date <= :tom::date)
            ORDER BY o.oppdatert
            OFFSET :offset
            LIMIT :limit;
            """,
            "oid" to behandletAvOid,
            "fom" to fom,
            "tom" to tom,
            "offset" to offset,
            "limit" to limit,
        ).list { row ->
            BehandletOppgaveFraDatabaseForVisning(
                id = row.long("oppgave_id"),
                aktørId = row.string("aktør_id"),
                fødselsnummer = row.string("fødselsnummer"),
                egenskaper =
                    row
                        .array<String>("egenskaper")
                        .map { enumValueOf<EgenskapForDatabase>(it) }
                        .toSet(),
                ferdigstiltTidspunkt = row.localDateTime("ferdigstilt_tidspunkt"),
                ferdigstiltAv = row.stringOrNull("ferdigstilt_av"),
                saksbehandler = row.stringOrNull("saksbehandler") ?: row.stringOrNull("ferdigstilt_av"),
                beslutter = row.stringOrNull("beslutter"),
                navn =
                    PersonnavnFraDatabase(
                        row.string("fornavn"),
                        row.stringOrNull("mellomnavn"),
                        row.string("etternavn"),
                    ),
                filtrertAntall = row.int("filtered_count"),
            )
        }

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
