package no.nav.helse.modell.command

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.helse.Oppgavestatus
import no.nav.helse.modell.vedtak.EnhetDto
import no.nav.helse.modell.vedtak.NavnDto
import no.nav.helse.modell.vedtak.SaksbehandleroppgaveDto
import no.nav.helse.modell.vedtak.Saksbehandleroppgavetype
import no.nav.helse.objectMapper
import java.util.*

fun Session.insertOppgave(
    eventId: UUID,
    oppgavetype: String,
    oppgavestatus: Oppgavestatus,
    ferdigstiltAv: String?,
    oid: UUID?,
    vedtakRef: Long?
) =
    this.run(
        queryOf(
            """
                INSERT INTO oppgave(event_id, oppdatert, type, status, ferdigstilt_av, ferdigstilt_av_oid, vedtak_ref)
                VALUES (?, now(), ?, CAST(? as oppgavestatus), ?, ?, ?);
            """,
            eventId,
            oppgavetype,
            oppgavestatus.name,
            ferdigstiltAv,
            oid,
            vedtakRef
        ).asUpdate
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

fun Session.findSaksbehandlerOppgaver(): List<SaksbehandleroppgaveDto> = this.run(
    queryOf(
        """
            SELECT *, (SELECT json_agg(distinct melding) meldinger FROM warning where spleisbehov_ref=o.event_id), sot.type as saksbehandleroppgavetype
            FROM oppgave o
                   INNER JOIN vedtak v on o.vedtak_ref = v.id
                   INNER JOIN person p on v.person_ref = p.id
                   INNER JOIN person_info pi on p.info_ref = pi.id
                   LEFT JOIN (select navn as enhet_navn, id as enhet_id from enhet) e on p.enhet_ref = enhet_id
                   LEFT JOIN saksbehandleroppgavetype sot on o.event_id = sot.spleisbehov_ref
            WHERE status = 'AvventerSaksbehandler'::oppgavestatus and (sot.type IS NULL OR sot.type != 'FØRSTEGANGSBEHANDLING')
            ORDER BY opprettet DESC
            LIMIT 500
        """
    )
        .map(::saksbehandleroppgaveDto)
        .asList
)

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

private fun saksbehandleroppgaveDto(it: Row) = SaksbehandleroppgaveDto(
    spleisbehovId = UUID.fromString(it.string("event_id")),
    opprettet = it.localDateTime("opprettet"),
    vedtaksperiodeId = UUID.fromString(it.string("vedtaksperiode_id")),
    periodeFom = it.localDate("fom"),
    periodeTom = it.localDate("tom"),
    navn = NavnDto(
        it.string("fornavn"),
        it.stringOrNull("mellomnavn"),
        it.string("etternavn")
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
