package no.nav.helse.db

import kotliquery.sessionOf
import java.util.UUID
import javax.sql.DataSource

class TildelingDao(private val dataSource: DataSource) : TildelingRepository {
    override fun tildel(
        oppgaveId: Long,
        saksbehandlerOid: UUID,
    ) {
        sessionOf(dataSource).use { session ->
            session.transaction { transaction ->
                TransactionalTildelingDao(transaction).tildel(oppgaveId, saksbehandlerOid)
            }
        }
    }

    override fun avmeld(oppgaveId: Long) {
        sessionOf(dataSource).use { session ->
            session.transaction { transaction ->
                TransactionalTildelingDao(transaction).avmeld(oppgaveId)
            }
        }
    }

    override fun tildelingForPerson(fødselsnummer: String): TildelingDto? {
        sessionOf(dataSource).use { session ->
            session.transaction { transaction ->
                return TransactionalTildelingDao(transaction).tildelingForPerson(fødselsnummer)
            }
        }
    }

    override fun tildelingForOppgave(oppgaveId: Long): TildelingDto? {
        sessionOf(dataSource).use { session ->
            session.transaction { transaction ->
                return TransactionalTildelingDao(transaction).tildelingForOppgave(oppgaveId)
            }
        }
    }
}
