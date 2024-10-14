package no.nav.helse.modell.gosysoppgaver

import kotliquery.sessionOf
import no.nav.helse.db.TransactionalÅpneGosysOppgaverDao
import no.nav.helse.db.ÅpneGosysOppgaverRepository
import javax.sql.DataSource

internal class ÅpneGosysOppgaverDao(val dataSource: DataSource) : ÅpneGosysOppgaverRepository {
    override fun persisterÅpneGosysOppgaver(åpneGosysOppgaver: ÅpneGosysOppgaverDto) {
        sessionOf(dataSource).use { session ->
            session.transaction { transactionalSession ->
                TransactionalÅpneGosysOppgaverDao(transactionalSession).persisterÅpneGosysOppgaver(åpneGosysOppgaver)
            }
        }
    }

    override fun harÅpneOppgaver(fødselsnummer: String): Int? {
        return sessionOf(dataSource).use { session ->
            session.transaction { transactionalSession ->
                TransactionalÅpneGosysOppgaverDao(transactionalSession).harÅpneOppgaver(fødselsnummer)
            }
        }
    }
}
