package no.nav.helse.modell.dao

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.Løsningstype
import no.nav.helse.modell.dto.OppgaveDto
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class OppgaveDao(private val dataSource: DataSource) {

    fun insertOppgave(behovId: UUID, oppgavetype: String, løsningstype: Løsningstype): Long? =
        using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO oppgave(behov_id, type, løsningstype) VALUES(?, ?, CAST(? as løsningstype_type));",
                    behovId,
                    oppgavetype,
                    løsningstype.name
                ).asUpdateAndReturnGeneratedKey
            )
        }

    fun updateOppgave(behovId: UUID, oppgavetype: String, ferdigstilt: LocalDateTime?) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "UPDATE oppgave SET ferdigstilt=? WHERE behov_id=? AND type=?",
                    ferdigstilt,
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
        session.run(queryOf("SELECT * FROM oppgave WHERE behov_id=? AND ferdigstilt=NULL", behovId)
            .map {
                OppgaveDto(
                    it.long("id"),
                    it.localDateTime("ferdigstilt"),
                    it.string("type"),
                    UUID.fromString(it.string("behov_id"))
                )
            }
            .asSingle)
    }
}
