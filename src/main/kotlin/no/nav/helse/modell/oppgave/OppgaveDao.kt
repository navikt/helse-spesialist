package no.nav.helse.modell.oppgave

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.mediator.meldinger.Kjønn
import no.nav.helse.modell.oppgave.Oppgavestatus.AvventerSaksbehandler
import no.nav.helse.modell.vedtak.EnhetDto
import no.nav.helse.modell.vedtak.OppgaveDto
import no.nav.helse.modell.vedtak.PersoninfoDto
import no.nav.helse.modell.vedtak.TildelingDto
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

internal class OppgaveDao(private val dataSource: DataSource) {
    internal fun finnOppgaver(inkluderRiskQaOppgaver: Boolean) =
        using(sessionOf(dataSource)) { session ->
            val eventuellEkskluderingAvRiskQA = if (inkluderRiskQaOppgaver) "" else "AND o.type != 'RISK_QA'"

            @Language("PostgreSQL")
            val query = """
            SELECT o.id as oppgave_id, o.type AS oppgavetype, COUNT(DISTINCT w.melding) as antall_varsler, o.opprettet, s.epost, s.navn as saksbehandler_navn, s.oid, v.vedtaksperiode_id, v.fom, v.tom, pi.fornavn, pi.mellomnavn, pi.etternavn, pi.fodselsdato,
                   pi.kjonn, p.aktor_id, p.fodselsnummer, sot.type as saksbehandleroppgavetype, sot.inntektskilde, e.id AS enhet_id, e.navn AS enhet_navn, t.på_vent
            FROM oppgave o
                INNER JOIN vedtak v ON o.vedtak_ref = v.id
                INNER JOIN person p ON v.person_ref = p.id
                INNER JOIN person_info pi ON p.info_ref = pi.id
                LEFT JOIN warning w ON w.vedtak_ref = v.id
                LEFT JOIN enhet e ON p.enhet_ref = e.id
                LEFT JOIN saksbehandleroppgavetype sot ON v.id = sot.vedtak_ref
                LEFT JOIN tildeling t ON o.id = t.oppgave_id_ref AND (t.gyldig_til IS NULL OR t.gyldig_til > now())
                LEFT JOIN saksbehandler s on t.saksbehandler_ref = s.oid
            WHERE status = 'AvventerSaksbehandler'::oppgavestatus
            $eventuellEkskluderingAvRiskQA
                GROUP BY o.id, o.opprettet, s.oid, s.epost, v.vedtaksperiode_id, v.fom, v.tom, pi.fornavn, pi.mellomnavn, pi.etternavn, pi.fodselsdato, pi.kjonn, p.aktor_id, p.fodselsnummer, sot.type, sot.inntektskilde, e.id, e.navn, t.saksbehandler_ref, t.på_vent
                ORDER BY
                    CASE WHEN t.saksbehandler_ref IS NOT NULL THEN 0 ELSE 1 END,
                    CASE WHEN sot.type = 'FORLENGELSE' OR sot.type = 'INFOTRYGDFORLENGELSE' THEN 0 ELSE 1 END,
                opprettet ASC
            LIMIT 1000;
    """
            session.run(
                queryOf(query)
                    .map(::saksbehandleroppgaveDto)
                    .asList
            )
        }

    internal fun finnOppgaveId(vedtaksperiodeId: UUID) =
        using(sessionOf(dataSource)) {
            @Language("PostgreSQL")
            val statement = """
                SELECT id FROM oppgave
                WHERE vedtak_ref =
                    (SELECT id FROM vedtak WHERE vedtaksperiode_id = ?)
                AND status = 'AvventerSaksbehandler'::oppgavestatus
                """
            it.run(
                queryOf(statement, vedtaksperiodeId)
                    .map { it.long("id") }.asSingle
            )
        }

    internal fun finnOppgaveId(fødselsnummer: String) =
        using(sessionOf(dataSource)) { session ->
            @Language("PostgreSQL")
            val query =
                """
                    SELECT o.id as oppgaveId
                    FROM oppgave o
                             JOIN vedtak v ON v.id = o.vedtak_ref
                             JOIN person p ON v.person_ref = p.id
                    WHERE o.status = 'AvventerSaksbehandler'::oppgavestatus
                      AND p.fodselsnummer = :fodselsnummer;
                """
            session.run(
                queryOf(query, mapOf("fodselsnummer" to fødselsnummer.toLong()))
                    .map { it.long("oppgaveId") }.asSingle
            )
        }

    internal fun finn(oppgaveId: Long) = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val statement = """
            SELECT o.type, o.status, v.vedtaksperiode_id, o.ferdigstilt_av, o.ferdigstilt_av_oid, o.utbetaling_id
            FROM oppgave o
            INNER JOIN vedtak v on o.vedtak_ref = v.id
            WHERE o.id = ?
        """
        session.run(
            queryOf(statement, oppgaveId)
                .map { row ->
                    Oppgave(
                        id = oppgaveId,
                        type = row.string("type"),
                        status = enumValueOf(row.string("status")),
                        vedtaksperiodeId = UUID.fromString(row.string("vedtaksperiode_id")),
                        utbetalingId = row.stringOrNull("utbetaling_id")?.let(UUID::fromString),
                        ferdigstiltAvIdent = row.stringOrNull("ferdigstilt_av"),
                        ferdigstiltAvOid = row.stringOrNull("ferdigstilt_av_oid")?.let(UUID::fromString)
                    )
                }.asSingle
        )
    }

    internal fun finnAktive(vedtaksperiodeId: UUID) = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val statement = """
            SELECT o.id, o.type, o.status, o.utbetaling_id
            FROM oppgave o
            INNER JOIN vedtak v on o.vedtak_ref = v.id
            WHERE v.vedtaksperiode_id = ? AND o.status IN('AvventerSystem'::oppgavestatus, 'AvventerSaksbehandler'::oppgavestatus)
        """
        session.run(
            queryOf(statement, vedtaksperiodeId)
                .map { row ->
                    Oppgave(
                        id = row.long("id"),
                        type = row.string("type"),
                        status = enumValueOf(row.string("status")),
                        vedtaksperiodeId = vedtaksperiodeId,
                        utbetalingId = row.stringOrNull("utbetaling_id")?.let(UUID::fromString),
                    )
                }.asList
        )
    }

    internal fun finn(fødselsnummer: String) = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val statement = """
            SELECT o.id, o.type, o.status, v.vedtaksperiode_id, o.utbetaling_id
            FROM oppgave o
            INNER JOIN vedtak v on o.vedtak_ref = v.id
            INNER JOIN person p on v.person_ref = p.id
            WHERE p.fodselsnummer = ?
        """
        session.run(
            queryOf(statement, fødselsnummer)
                .map { row ->
                    Oppgave(
                        id = row.long("id"),
                        type = row.string("type"),
                        status = enumValueOf(row.string("status")),
                        vedtaksperiodeId = UUID.fromString(row.string("vedtaksperiode_id")),
                        utbetalingId = row.stringOrNull("utbetaling_id")?.let(UUID::fromString),
                    )
                }.asList
        )
    }

    internal fun finnVedtaksperiodeId(oppgaveId: Long) = requireNotNull(using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val statement = """
            SELECT v.vedtaksperiode_id
            FROM vedtak v
            INNER JOIN oppgave o on v.id = o.vedtak_ref
            WHERE o.id = ?
        """
        session.run(
            queryOf(
                statement,
                oppgaveId
            ).map { row -> UUID.fromString(row.string("vedtaksperiode_id")) }.asSingle
        )
    })

    internal fun opprettOppgave(
        commandContextId: UUID,
        oppgavetype: String,
        vedtakRef: Long?,
        utbetalingId: UUID
        ) = requireNotNull(using(sessionOf(dataSource, returnGeneratedKey = true)) {
        it.run(
            queryOf(
                """
                INSERT INTO oppgave(oppdatert, type, status, ferdigstilt_av, ferdigstilt_av_oid, vedtak_ref, command_context_id, utbetaling_id)
                VALUES (now(), CAST(? as oppgavetype), CAST(? as oppgavestatus), ?, ?, ?, ?, ?);
            """,
                oppgavetype,
                AvventerSaksbehandler.name,
                null,
                null,
                vedtakRef,
                commandContextId,
                utbetalingId
            ).asUpdateAndReturnGeneratedKey
        )
    }) { "Kunne ikke opprette oppgave" }

    internal fun updateOppgave(
        oppgaveId: Long,
        oppgavestatus: Oppgavestatus,
        ferdigstiltAv: String? = null,
        oid: UUID? = null
    ) = using(sessionOf(dataSource)) {
        it.run(
            queryOf(
                "UPDATE oppgave SET oppdatert=now(), ferdigstilt_av=?, ferdigstilt_av_oid=?, status=?::oppgavestatus WHERE id=?",
                ferdigstiltAv,
                oid,
                oppgavestatus.name,
                oppgaveId
            ).asUpdate
        )
    }

    internal fun finnContextId(oppgaveId: Long) = requireNotNull(using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf("SELECT command_context_id FROM oppgave WHERE id = ?", oppgaveId)
                .map { row -> UUID.fromString(row.string("command_context_id")) }.asSingle
        )
    })

    internal fun finnHendelseId(oppgaveId: Long) = requireNotNull(using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val statement =
            """SELECT hendelse_id FROM command_context WHERE context_id = (SELECT command_context_id FROM oppgave WHERE id = ?)"""
        session.run(
            queryOf(statement, oppgaveId)
                .map { row -> UUID.fromString(row.string("hendelse_id")) }.asSingle
        )
    })

    internal fun harAktivOppgave(vedtaksperiodeId: UUID) = requireNotNull(using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val query = """
                SELECT COUNT(1) AS oppgave_count FROM oppgave o
                INNER JOIN vedtak v on o.vedtak_ref = v.id
                WHERE v.vedtaksperiode_id = ? AND o.status IN('AvventerSystem'::oppgavestatus, 'AvventerSaksbehandler'::oppgavestatus)
            """
        session.run(queryOf(query, vedtaksperiodeId).map { it.int("oppgave_count") }.asSingle)
    }) > 0

    internal fun harFerdigstiltOppgave(vedtaksperiodeId: UUID) =
        requireNotNull(using(sessionOf(dataSource)) { session ->
            @Language("PostgreSQL")
            val query = """
                SELECT COUNT(1) AS oppgave_count FROM oppgave o
                INNER JOIN vedtak v on o.vedtak_ref = v.id
                WHERE v.vedtaksperiode_id = ? AND o.status = 'Ferdigstilt'::oppgavestatus
            """
            session.run(queryOf(query, vedtaksperiodeId).map { it.int("oppgave_count") }.asSingle)
        }) > 0

    internal fun venterPåSaksbehandler(oppgaveId: Long) = requireNotNull(using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val query = """
                SELECT EXISTS ( SELECT 1 FROM oppgave WHERE id=? AND status IN('AvventerSaksbehandler'::oppgavestatus) )
            """
        session.run(queryOf(query, oppgaveId).map { it.boolean(1) }.asSingle)
    })

    private fun saksbehandleroppgaveDto(it: Row) = OppgaveDto(
        oppgavereferanse = it.string("oppgave_id"),
        oppgavetype = it.string("oppgavetype"),
        opprettet = it.localDateTime("opprettet"),
        vedtaksperiodeId = UUID.fromString(it.string("vedtaksperiode_id")),
        personinfo = PersoninfoDto(
            it.string("fornavn"),
            it.stringOrNull("mellomnavn"),
            it.string("etternavn"),
            it.localDateOrNull("fodselsdato"),
            it.stringOrNull("kjonn")?.let(Kjønn::valueOf)
        ),
        aktørId = it.long("aktor_id").toString(),
        fødselsnummer = it.long("fodselsnummer").toFødselsnummer(),
        antallVarsler = it.int("antall_varsler"),
        type = it.stringOrNull("saksbehandleroppgavetype")?.let(Periodetype::valueOf),
        inntektskilde = it.stringOrNull("inntektskilde")?.let(Inntektskilde::valueOf),
        boenhet = EnhetDto(it.string("enhet_id"), it.string("enhet_navn")),
        tildeling = it.stringOrNull("epost")?.let { epost ->
            TildelingDto(
                navn = it.string("saksbehandler_navn"),
                epost = epost,
                oid = UUID.fromString(it.string("oid")),
                påVent = it.boolean("på_vent")
            )
        }
    )

    private fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()

    internal fun finnFødselsnummer(oppgaveId: Long) = requireNotNull(using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val statement = """
                SELECT fodselsnummer from person
                INNER JOIN vedtak v on person.id = v.person_ref
                INNER JOIN oppgave o on v.id = o.vedtak_ref
                WHERE o.id = ?
            """

        session.run(
            queryOf(
                statement,
                oppgaveId
            ).map { it.long("fodselsnummer").toFødselsnummer() }.asSingle
        )
    })
}
