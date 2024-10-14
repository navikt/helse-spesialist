package no.nav.helse.db

import kotliquery.sessionOf
import no.nav.helse.HelseDao
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import java.util.UUID
import javax.sql.DataSource

class NotatDao(private val dataSource: DataSource) : HelseDao(dataSource), NotatRepository {
    override fun lagreForOppgaveId(
        oppgaveId: Long,
        tekst: String,
        saksbehandler_oid: UUID,
        notatType: NotatType,
    ): Long? {
        return sessionOf(dataSource).use { session ->
            session.transaction { transactionalSession ->
                TransactionalNotatDao(transactionalSession).lagreForOppgaveId(oppgaveId, tekst, saksbehandler_oid, notatType)
            }
        }
    }
}
