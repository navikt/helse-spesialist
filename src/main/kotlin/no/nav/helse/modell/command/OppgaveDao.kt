package no.nav.helse.modell.command

import kotliquery.*
import no.nav.helse.Oppgavestatus
import no.nav.helse.Oppgavestatus.*
import no.nav.helse.modell.person.Kjønn
import no.nav.helse.modell.vedtak.EnhetDto
import no.nav.helse.modell.vedtak.PersoninfoDto
import no.nav.helse.modell.vedtak.SaksbehandleroppgaveDto
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

internal class OppgaveDao(private val dataSource: DataSource) {
    internal fun finnOppgaver() =
        using(sessionOf(dataSource)) { session ->
            session.findSaksbehandlerOppgaver()
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

    internal fun opprettOppgave(
        eventId: UUID,
        commandContextId: UUID,
        oppgavetype: String,
        vedtakRef: Long?
    ) = requireNotNull(using(sessionOf(dataSource, returnGeneratedKey = true)) {
        it.insertOppgave(eventId, oppgavetype, AvventerSaksbehandler, null, null, vedtakRef, commandContextId)
    }) { "Kunne ikke opprette oppgave" }

    internal fun updateOppgave(
        oppgaveId: Long,
        oppgavestatus: Oppgavestatus,
        ferdigstiltAv: String?,
        oid: UUID?
    ) = using(sessionOf(dataSource)) {
        it.run(
            queryOf("UPDATE oppgave SET oppdatert=now(), ferdigstilt_av=?, ferdigstilt_av_oid=?, status=?::oppgavestatus WHERE id=?",
                ferdigstiltAv,
                oid,
                oppgavestatus.name,
                oppgaveId
            ).asUpdate)
    }

    internal fun finnContextId(oppgaveId: Long) = requireNotNull(using(sessionOf(dataSource)) { session ->
        session.run(queryOf("SELECT command_context_id FROM oppgave WHERE id = ?", oppgaveId)
            .map { row -> UUID.fromString(row.string("command_context_id"))}.asSingle)
    })

    internal fun finnHendelseId(oppgaveId: Long) = requireNotNull(using(sessionOf(dataSource)) { session ->
        session.run(queryOf("SELECT hendelse_id FROM oppgave WHERE id = ?", oppgaveId)
            .map { row -> UUID.fromString(row.string("hendelse_id")) }.asSingle)
    })

    internal fun harAktivOppgave(oppgaveId: Long) = requireNotNull(using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val query = """
                SELECT EXISTS (
                    SELECT 1
                    FROM oppgave
                    WHERE id=?
                      AND status IN('AvventerSystem'::oppgavestatus, 'AvventerSaksbehandler'::oppgavestatus, 'Invalidert'::oppgavestatus)
                )
            """
        session.run(queryOf(query, oppgaveId).map { it.boolean(1) }.asSingle)
    })

    internal fun invaliderOppgaver(vedtaksperiodeId: UUID) = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val query = """
            UPDATE oppgave SET status = ?::oppgavestatus, oppdatert=now()
            WHERE vedtak_ref in (SELECT id FROM vedtak WHERE vedtaksperiode_id = ?)
            AND status != ?::oppgavestatus
        """
        session.run(queryOf(query, Invalidert.name, vedtaksperiodeId, Ferdigstilt.name).asUpdate)
    }

    internal fun invaliderOppgaver(fødselsnummer: String) = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val query = """
            UPDATE oppgave SET status = :nyStatus::oppgavestatus, oppdatert=now()
            WHERE vedtak_ref in (SELECT id FROM vedtak WHERE person_ref = (
                SELECT id FROM person WHERE fodselsnummer = :fodselsnummer
            ))
            AND status != :ignorertStatus::oppgavestatus
        """
        session.run(
            queryOf(
                query, mapOf(
                    "nyStatus" to Invalidert.name,
                    "fodselsnummer" to fødselsnummer.toLong(),
                    "ignorertStatus" to Ferdigstilt.name
                )
            ).asUpdate
        )
    }
}

fun Session.insertOppgave(
    eventId: UUID,
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
                INSERT INTO oppgave(hendelse_id, oppdatert, type, status, ferdigstilt_av, ferdigstilt_av_oid, vedtak_ref, command_context_id)
                VALUES (?, now(), ?, CAST(? as oppgavestatus), ?, ?, ?, ?);
            """,
            eventId,
            oppgavetype,
            oppgavestatus.name,
            ferdigstiltAv,
            oid,
            vedtakRef,
            commandContextId
        ).asUpdateAndReturnGeneratedKey
    )

fun Session.updateOppgave(
    eventId: UUID,
    oppgavetype: String,
    oppgavestatus: Oppgavestatus,
    ferdigstiltAv: String?,
    oid: UUID?
) =
    this.run(
        queryOf(
            """
                UPDATE oppgave SET oppdatert=now(), ferdigstilt_av=?, ferdigstilt_av_oid=?, status=?::oppgavestatus
                WHERE hendelse_id=? AND type=?;
            """,
            ferdigstiltAv,
            oid,
            oppgavestatus.name,
            eventId,
            oppgavetype
        ).asUpdate
    )


fun Session.findNåværendeOppgave(eventId: UUID): OppgaveDto? = this.run(
    queryOf(
        """
            SELECT *
            FROM oppgave
            WHERE hendelse_id=?
              AND status IN('AvventerSystem'::oppgavestatus, 'AvventerSaksbehandler'::oppgavestatus, 'Invalidert'::oppgavestatus)
        """,
        eventId
    )
        .map(::oppgaveDto)
        .asSingle
)

fun Session.finnOppgaveId(vedtaksperiodeId: UUID): Long? {
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
SELECT *,
       (SELECT json_agg(DISTINCT melding) meldinger FROM warning WHERE hendelse_id = o.hendelse_id),
       sot.type AS saksbehandleroppgavetype,
       o.id AS oppgave_id
FROM oppgave o
         INNER JOIN vedtak v ON o.vedtak_ref = v.id
         INNER JOIN person p ON v.person_ref = p.id
         INNER JOIN person_info pi ON p.info_ref = pi.id
         LEFT JOIN (SELECT navn AS enhet_navn, id AS enhet_id FROM enhet) e ON p.enhet_ref = enhet_id
         LEFT JOIN saksbehandleroppgavetype sot ON o.hendelse_id = sot.hendelse_id
         LEFT JOIN tildeling t ON o.id = t.oppgave_id_ref AND (t.gyldig_til IS NULL OR t.gyldig_til > now())
         LEFT JOIN saksbehandler s on t.saksbehandler_ref = s.oid
WHERE status = 'AvventerSaksbehandler'::oppgavestatus
ORDER BY CASE WHEN t.saksbehandler_ref IS NOT NULL THEN 0 ELSE 1 END, CASE WHEN sot.type = 'FORLENGELSE' OR sot.type = 'INFOTRYGDFORLENGELSE' THEN 0 ELSE 1 END, opprettet DESC
LIMIT 500
"""
    return this.run(
        queryOf(query)
            .map(::saksbehandleroppgaveDto)
            .asList
    )
}

fun Session.findOppgave(fødselsnummer: String): OppgaveDto? {
    @Language("PostgreSQL")
    val query = """
SELECT o.*
FROM oppgave o
         JOIN vedtak v ON v.id = o.vedtak_ref
         JOIN person p ON v.person_ref = p.id
WHERE o.status = 'AvventerSaksbehandler'::oppgavestatus
  AND p.fodselsnummer = ?;
"""
    return run(queryOf(query, fødselsnummer.toLong()).map {
        OppgaveDto(
            id = it.long("id"),
            opprettet = it.localDateTime("opprettet"),
            oppdatert = it.localDateTime("oppdatert"),
            eventId = UUID.fromString(it.string("hendelse_id")),
            oppgaveType = it.string("type"),
            status = Oppgavestatus.valueOf(it.string("status")),
            vedtaksref = it.longOrNull("vedtak_ref")
        )
    }.asSingle)
}

fun Session.eventIdForVedtaksperiode(vedtaksperiodeId: UUID) = this.run(
    queryOf(
        """
            SELECT hendelse_id
            FROM oppgave o
                INNER JOIN vedtak v on o.vedtak_ref = v.id
            WHERE v.vedtaksperiode_id = ? AND status = 'AvventerSaksbehandler'::oppgavestatus
        """,
        vedtaksperiodeId
    )
        .map { UUID.fromString(it.stringOrNull("hendelse_id")) }
        .asSingle
)


fun Session.invaliderSaksbehandleroppgaver(fødselsnummer: String, orgnummer: String) {
    @Language("PostgreSQL")
    val finnOppgaveIder = """
SELECT o.*
FROM vedtak v
         JOIN oppgave o ON o.vedtak_ref = v.id
         JOIN person p ON v.person_ref = p.id
         JOIN arbeidsgiver a ON v.arbeidsgiver_ref = a.id
WHERE a.orgnummer = :orgnummer
  AND p.fodselsnummer = :fodselsnummer
  AND o.status = 'AvventerSaksbehandler'::oppgavestatus;
"""

    @Language("PostgreSQL")
    val invaliderOppgave = "UPDATE oppgave SET status = 'Invalidert'::oppgavestatus WHERE id=:id;"

    run(
        queryOf(
            finnOppgaveIder,
            mapOf("orgnummer" to orgnummer.toLong(), "fodselsnummer" to fødselsnummer.toLong())
        ).map { it.long("id") }.asList
    ).forEach { id -> run(queryOf(invaliderOppgave, mapOf("id" to id)).asUpdate) }
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
    antallVarsler = objectMapper.readTree(it.stringOrNull("meldinger") ?: "[]").count(),
    type = it.stringOrNull("saksbehandleroppgavetype")?.let { type -> Saksbehandleroppgavetype.valueOf(type) },
    boenhet = EnhetDto(it.string("enhet_id"), it.string("enhet_navn"))
)

private fun oppgaveDto(it: Row) = OppgaveDto(
    id = it.long("id"),
    opprettet = it.localDateTime("opprettet"),
    oppdatert = it.localDateTimeOrNull("oppdatert"),
    oppgaveType = it.string("type"),
    eventId = UUID.fromString(it.string("hendelse_id")),
    status = Oppgavestatus.valueOf(it.string("status")),
    vedtaksref = it.longOrNull("vedtak_ref")
)

private fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()
