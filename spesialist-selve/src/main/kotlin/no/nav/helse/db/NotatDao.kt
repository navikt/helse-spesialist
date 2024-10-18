package no.nav.helse.db

import kotliquery.sessionOf
import no.nav.helse.HelseDao
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.sql.DataSource

class NotatDao(private val dataSource: DataSource) : HelseDao(dataSource), NotatRepository {
    private companion object {
        private val log = LoggerFactory.getLogger(NotatDao::class.java)
    }

    override fun lagreForOppgaveId(
        oppgaveId: Long,
        tekst: String,
        saksbehandler_oid: UUID,
        notatType: NotatType,
    ): Long? {
        log.info("{} lagrer {} notat for oppgaveId {}", saksbehandler_oid, notatType, oppgaveId)
        return sessionOf(dataSource).use { session ->
            TransactionalNotatDao(session).lagreForOppgaveId(oppgaveId, tekst, saksbehandler_oid, notatType)
        }
    }
}
