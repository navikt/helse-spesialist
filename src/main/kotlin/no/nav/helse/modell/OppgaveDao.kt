package no.nav.helse.modell

import kotliquery.*
import no.nav.helse.mediator.meldinger.Kjønn
import no.nav.helse.modell.Oppgavestatus.AvventerSaksbehandler
import no.nav.helse.modell.vedtak.EnhetDto
import no.nav.helse.modell.vedtak.PersoninfoDto
import no.nav.helse.modell.vedtak.SaksbehandleroppgaveDto
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

internal class OppgaveDao(private val dataSource: DataSource) {
    internal fun finnOppgaver() =
        using(sessionOf(dataSource)) { session ->
            session.findSaksbehandlerOppgaver()
        }

    internal fun finnOppgaveId(vedtaksperiodeId: UUID) =
        using(sessionOf(dataSource)) { it.finnOppgaveId(vedtaksperiodeId) }

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
            SELECT o.type, o.status, v.vedtaksperiode_id
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
                        vedtaksperiodeId = UUID.fromString(row.string("vedtaksperiode_id"))
                    )
                }.asSingle
        )
    }

    internal fun finn(vedtaksperiodeId: UUID) = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val statement = """
            SELECT o.id, o.type, o.status
            FROM oppgave o
            INNER JOIN vedtak v on o.vedtak_ref = v.id
            WHERE v.vedtaksperiode_id = ?
        """
        session.run(
            queryOf(statement, vedtaksperiodeId)
                .map { row ->
                    Oppgave(
                        id = row.long("id"),
                        type = row.string("type"),
                        status = enumValueOf(row.string("status")),
                        vedtaksperiodeId = vedtaksperiodeId
                    )
                }.asList
        )
    }

    internal fun finn(fødselsnummer: String) = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val statement = """
            SELECT o.id, o.type, o.status, v.vedtaksperiode_id
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
                        vedtaksperiodeId = UUID.fromString(row.string("vedtaksperiode_id"))
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
        vedtakRef: Long?
    ) = requireNotNull(using(sessionOf(dataSource, returnGeneratedKey = true)) {
        it.insertOppgave(oppgavetype, AvventerSaksbehandler, null, null, vedtakRef, commandContextId)
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

    internal fun erAktivOppgave(oppgaveId: Long) = requireNotNull(using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val query = """
                SELECT EXISTS ( SELECT 1 FROM oppgave WHERE id=? AND status IN('AvventerSaksbehandler'::oppgavestatus) )
            """
        session.run(queryOf(query, oppgaveId).map { it.boolean(1) }.asSingle)
    })

    private fun Session.insertOppgave(
        oppgavetype: String,
        oppgavestatus: Oppgavestatus,
        ferdigstiltAv: String?,
        oid: UUID?,
        vedtakRef: Long?,
        commandContextId: UUID?
    ) =
        this.run(
            queryOf(
                """
                INSERT INTO oppgave(oppdatert, type, status, ferdigstilt_av, ferdigstilt_av_oid, vedtak_ref, command_context_id)
                VALUES (now(), ?, CAST(? as oppgavestatus), ?, ?, ?, ?);
            """,
                oppgavetype,
                oppgavestatus.name,
                ferdigstiltAv,
                oid,
                vedtakRef,
                commandContextId
            ).asUpdateAndReturnGeneratedKey
        )

    private fun Session.finnOppgaveId(vedtaksperiodeId: UUID): Long? {
        @Language("PostgreSQL")
        val statement = """
            SELECT id FROM oppgave
            WHERE vedtak_ref =
                (SELECT id FROM vedtak WHERE vedtaksperiode_id = ?)
            AND status = 'AvventerSaksbehandler'::oppgavestatus
            """
        return this.run(
            queryOf(statement, vedtaksperiodeId)
                .map { it.long("id") }.asSingle
        )
    }


    fun Session.findSaksbehandlerOppgaver(): List<SaksbehandleroppgaveDto> {
        @Language("PostgreSQL")
        val query = """
        SELECT o.id as oppgave_id, COUNT(DISTINCT w.melding) as antall_varsler, o.opprettet, s.epost, v.vedtaksperiode_id, v.fom, v.tom, pi.fornavn, pi.mellomnavn, pi.etternavn, pi.fodselsdato,
               pi.kjonn, p.aktor_id, p.fodselsnummer, sot.type as saksbehandleroppgavetype, e.id AS enhet_id, e.navn AS enhet_navn
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
        GROUP BY o.id, o.opprettet, s.epost, v.vedtaksperiode_id, v.fom, v.tom, pi.fornavn, pi.mellomnavn, pi.etternavn, pi.fodselsdato, pi.kjonn, p.aktor_id, p.fodselsnummer, sot.type, e.id, e.navn, t.saksbehandler_ref
        ORDER BY
            CASE WHEN t.saksbehandler_ref IS NOT NULL THEN 0 ELSE 1 END,
            CASE WHEN sot.type = 'FORLENGELSE' OR sot.type = 'INFOTRYGDFORLENGELSE' THEN 0 ELSE 1 END,
            opprettet DESC
        LIMIT 500;
"""
        return this.run(
            queryOf(query)
                .map(::saksbehandleroppgaveDto)
                .asList
        )
    }

    private fun saksbehandleroppgaveDto(it: Row) = SaksbehandleroppgaveDto(
        oppgavereferanse = it.long("oppgave_id"),
        saksbehandlerepost = it.stringOrNull("epost"),
        opprettet = it.localDateTime("opprettet"),
        vedtaksperiodeId = UUID.fromString(it.string("vedtaksperiode_id")),
        periodeFom = it.localDate("fom"),
        periodeTom = it.localDate("tom"),
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
        type = it.stringOrNull("saksbehandleroppgavetype")?.let { type -> Saksbehandleroppgavetype.valueOf(type) },
        boenhet = EnhetDto(it.string("enhet_id"), it.string("enhet_navn"))
    )

    private fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()
}
