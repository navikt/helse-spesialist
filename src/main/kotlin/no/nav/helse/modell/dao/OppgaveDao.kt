package no.nav.helse.modell.dao

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.Oppgavestatus
import no.nav.helse.modell.dto.NavnDto
import no.nav.helse.modell.dto.OppgaveDto
import no.nav.helse.modell.dto.SaksbehandleroppgaveDto
import no.nav.helse.objectMapper
import java.util.*
import javax.sql.DataSource

class OppgaveDao(private val dataSource: DataSource) {

    fun insertOppgave(behovId: UUID, oppgavetype: String, status: Oppgavestatus, vedtakRef: Int?) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO oppgave(behov_id, oppdatert, type, status, vedtak_ref) VALUES(?, now(), ?, CAST(? as oppgavestatus), ?);",
                    behovId,
                    oppgavetype,
                    status.name,
                    vedtakRef
                ).asUpdate
            )
        }

    fun updateOppgave(
        behovId: UUID,
        oppgavetype: String,
        oppgavestatus: Oppgavestatus,
        ferdigstiltAv: String?,
        oid: UUID?
    ) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "UPDATE oppgave SET oppdatert=now(), ferdigstilt_av=?, ferdigstilt_av_oid=?, status=?::oppgavestatus WHERE behov_id=? AND type=?;",
                    ferdigstiltAv,
                    oid,
                    oppgavestatus.name,
                    behovId,
                    oppgavetype
                ).asUpdate
            )
        }

    fun findOppgave(behovId: UUID, type: String): Long? =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT * FROM oppgave where behov_id=? AND type=?;",
                    behovId,
                    type
                ).map { it.long("id") }
                    .asSingle
            )
        }

    fun findNåværendeOppgave(behovId: UUID): OppgaveDto? = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "SELECT * FROM oppgave WHERE behov_id=? AND status IN('AvventerSystem'::oppgavestatus, 'AvventerSaksbehandler'::oppgavestatus)",
                behovId
            )
                .map(::oppgaveDto)
                .asSingle
        )
    }

    fun findSaksbehandlerOppgaver(): List<SaksbehandleroppgaveDto> = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                """
                SELECT *, (SELECT json_agg(melding) meldinger FROM warning where spleisbehov_ref=o.behov_id)
                FROM oppgave o
                       INNER JOIN vedtak v on o.vedtak_ref = v.id
                       INNER JOIN person p on v.person_ref = p.id
                       INNER JOIN person_info pi on p.info_ref = pi.id
                WHERE status = 'AvventerSaksbehandler'::oppgavestatus
                  AND o.opprettet > now()::date
            """
            )
                .map(::saksbehandleroppgaveDto)
                .asList
        )
    }

    private fun saksbehandleroppgaveDto(it: Row): SaksbehandleroppgaveDto {
        return SaksbehandleroppgaveDto(
            spleisbehovId = UUID.fromString(it.string("behov_id")),
            opprettet = it.localDateTime("opprettet"),
            vedtaksperiodeId = UUID.fromString(it.string("vedtaksperiode_id")),
            periodeFom = it.localDate("fom"),
            periodeTom = it.localDate("tom"),
            navn = NavnDto(it.string("fornavn"), it.stringOrNull("mellomnavn"), it.string("etternavn")),
            fødselsnummer = it.long("fodselsnummer").toFødselsnummer(),
            antallVarsler = objectMapper.readTree(it.stringOrNull("meldinger") ?: "[]").count()
        )
    }

    private fun oppgaveDto(it: Row): OppgaveDto {
        return OppgaveDto(
            id = it.long("id"),
            opprettet = it.localDateTime("opprettet"),
            oppdatert = it.localDateTimeOrNull("oppdatert"),
            oppgaveType = it.string("type"),
            behovId = UUID.fromString(it.string("behov_id")),
            status = Oppgavestatus.valueOf(it.string("status")),
            vedtaksref = it.longOrNull("vedtak_ref")
        )
    }

    private fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()
}
