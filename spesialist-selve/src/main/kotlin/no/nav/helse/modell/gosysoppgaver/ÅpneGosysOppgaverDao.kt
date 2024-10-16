package no.nav.helse.modell.gosysoppgaver

import kotliquery.sessionOf
import no.nav.helse.db.TransactionalÅpneGosysOppgaverDao
import no.nav.helse.db.ÅpneGosysOppgaverRepository
import javax.sql.DataSource

internal class ÅpneGosysOppgaverDao(val dataSource: DataSource) : ÅpneGosysOppgaverRepository {
    override fun persisterÅpneGosysOppgaver(åpneGosysOppgaver: ÅpneGosysOppgaverDto) {
        sessionOf(dataSource).use { session ->
            TransactionalÅpneGosysOppgaverDao(session).persisterÅpneGosysOppgaver(åpneGosysOppgaver)
        }
    }

    override fun antallÅpneOppgaver(fødselsnummer: String): Int? {
        return sessionOf(dataSource).use { session ->
            TransactionalÅpneGosysOppgaverDao(session).antallÅpneOppgaver(fødselsnummer)
        }
    }
}
