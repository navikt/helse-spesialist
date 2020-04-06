package no.nav.helse.modell.dao

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.Oppgavetype
import no.nav.helse.Statustype
import java.util.UUID
import javax.sql.DataSource

class OppgaveDao(private val dataSource: DataSource) {
    fun insertOppgave(behovId: UUID, oppgavetype: Oppgavetype, statusType: Statustype): Long? =
        using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO oppgave(behov_id, type, status) VALUES(?, CAST(? as oppgave_type), CAST(? as oppgave_status));",
                    behovId,
                    oppgavetype.name,
                    statusType.name
                ).asUpdateAndReturnGeneratedKey
            )
        }
}
