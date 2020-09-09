package no.nav.helse.modell.command

import kotliquery.*
import no.nav.helse.Oppgavestatus
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
    internal fun hentSaksbehandlerOppgaver() =
        using(sessionOf(dataSource)) { session ->
            session.findSaksbehandlerOppgaver()
        }

    internal fun hentSaksbehandlerOppgave(fødselsnummer: String) =
        using(sessionOf(dataSource)) {
            it.findOppgave(fødselsnummer)
        }

    internal fun insertOppgave(
        eventId: UUID,
        commandContextId: UUID,
        oppgavetype: String,
        oppgavestatus: Oppgavestatus,
        ferdigstiltAv: String?,
        oid: UUID?,
        vedtakRef: Long?
    ) = requireNotNull(using(sessionOf(dataSource, returnGeneratedKey = true)) {
        it.insertOppgave(eventId, oppgavetype, oppgavestatus, ferdigstiltAv, oid, vedtakRef, commandContextId)
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
                INSERT INTO oppgave(event_id, oppdatert, type, status, ferdigstilt_av, ferdigstilt_av_oid, vedtak_ref, command_context_id)
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
                WHERE event_id=? AND type=?;
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
            WHERE event_id=?
              AND status IN('AvventerSystem'::oppgavestatus, 'AvventerSaksbehandler'::oppgavestatus, 'Invalidert'::oppgavestatus)
        """,
        eventId
    )
        .map(::oppgaveDto)
        .asSingle
)

fun Session.findSaksbehandlerOppgaver(): List<SaksbehandleroppgaveDto> {
    @Language("PostgreSQL")
    val query = """
SELECT *,
       (SELECT json_agg(DISTINCT melding) meldinger FROM warning WHERE spleisbehov_ref = o.event_id),
       sot.type AS saksbehandleroppgavetype
FROM oppgave o
         INNER JOIN vedtak v ON o.vedtak_ref = v.id
         INNER JOIN person p ON v.person_ref = p.id
         INNER JOIN person_info pi ON p.info_ref = pi.id
         LEFT JOIN (SELECT navn AS enhet_navn, id AS enhet_id FROM enhet) e ON p.enhet_ref = enhet_id
         LEFT JOIN saksbehandleroppgavetype sot ON o.event_id = sot.spleisbehov_ref
         LEFT JOIN tildeling t ON o.event_id = t.oppgave_ref
         LEFT JOIN saksbehandler s on t.saksbehandler_ref = s.oid
WHERE status = 'AvventerSaksbehandler'::oppgavestatus
ORDER BY CASE WHEN t.saksbehandler_ref IS NOT NULL THEN 0 ELSE 1 END, CASE WHEN sot.type = 'FORLENGELSE' THEN 0 ELSE 1 END, opprettet DESC
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
            eventId = UUID.fromString(it.string("event_id")),
            oppgaveType = it.string("type"),
            status = Oppgavestatus.valueOf(it.string("status")),
            vedtaksref = it.longOrNull("vedtak_ref")
        )
    }.asSingle)
}

fun Session.eventIdForVedtaksperiode(vedtaksperiodeId: UUID) = this.run(
    queryOf(
        """
            SELECT event_id
            FROM oppgave o
                INNER JOIN vedtak v on o.vedtak_ref = v.id
            WHERE v.vedtaksperiode_id = ? AND status = 'AvventerSaksbehandler'::oppgavestatus
        """,
        vedtaksperiodeId
    )
        .map { UUID.fromString(it.stringOrNull("event_id")) }
        .asSingle
)


fun Session.invaliderSaksbehandlerOppgaver(fødselsnummer: String, orgnummer: String) {
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
    oppgavereferanse = UUID.fromString(it.string("event_id")),
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
    eventId = UUID.fromString(it.string("event_id")),
    status = Oppgavestatus.valueOf(it.string("status")),
    vedtaksref = it.longOrNull("vedtak_ref")
)

private fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()
