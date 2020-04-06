package no.nav.helse.modell.dao

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.Statustype
import java.util.UUID
import javax.sql.DataSource

class OppgaveDao(private val dataSource: DataSource) {

    fun insertOppgave(behovId: UUID, oppgavetype: String, statusType: Statustype): Long? =
        using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO oppgave(behov_id, type, status) VALUES(?, ?, CAST(? as oppgave_status));",
                    behovId,
                    oppgavetype,
                    statusType.name
                ).asUpdateAndReturnGeneratedKey
            )
        }

    fun findOppgave(behovId: UUID): Long? =
        using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
            session.run(
                queryOf(
                    "SELECT * FROM oppgave where behov_id=?;",
                    behovId
                ).map { it.long("id") }
                    .asSingle
            )
        }

    fun updateOppgaveBesvart(spleisbehovId: UUID) =
        using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
            session.run(
                queryOf(
                    "UPDATE oppgave SET besvart=now() where behov_id=?;",
                    spleisbehovId
                ).asUpdate
            )
        }
}
