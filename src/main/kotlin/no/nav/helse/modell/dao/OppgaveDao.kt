package no.nav.helse.modell.dao

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.Oppgavestatus
import no.nav.helse.modell.dto.OppgaveDto
import java.util.UUID
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

    fun updateOppgave(behovId: UUID, oppgavetype: String, oppgavestatus: Oppgavestatus, ferdigstiltAv: String?) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "UPDATE oppgave SET oppdatert=now(), ferdigstilt_av=?, status=?::oppgavestatus WHERE behov_id=? AND type=?;",
                    ferdigstiltAv,
                    oppgavetype,
                    behovId,
                    oppgavestatus.name
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
            queryOf("SELECT * FROM oppgave WHERE behov_id=? AND status IN('AvventerSystem'::oppgavestatus, 'AvventerSaksbehandler'::oppgavestatus)", behovId)
                .map(::oppgaveDto)
                .asSingle
        )
    }

    fun findSaksbehandlerOppgaver(): List<OppgaveDto>? = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf("SELECT * FROM oppgave WHERE status='AvventerSaksbehandler'::oppgavestatus")
                .map(::oppgaveDto)
                .asList
        )
    }

    private fun oppgaveDto(it: Row): OppgaveDto {
        return OppgaveDto(
            id = it.long("id"),
            oppdatert = it.localDateTimeOrNull("oppdatert"),
            oppgaveType = it.string("type"),
            behovId = UUID.fromString(it.string("behov_id")),
            status = Oppgavestatus.valueOf(it.string("status")),
            vedtaksref = it.longOrNull("vedtak_ref")
        )
    }
}
