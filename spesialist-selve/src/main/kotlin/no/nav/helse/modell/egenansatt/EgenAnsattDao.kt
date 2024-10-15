package no.nav.helse.modell.egenansatt

import kotliquery.sessionOf
import no.nav.helse.db.EgenAnsattRepository
import no.nav.helse.db.TransactionalEgenAnsattDao
import java.time.LocalDateTime
import javax.sql.DataSource

class EgenAnsattDao(private val dataSource: DataSource) : EgenAnsattRepository {
    override fun lagre(
        fødselsnummer: String,
        erEgenAnsatt: Boolean,
        opprettet: LocalDateTime,
    ) {
        sessionOf(dataSource).use { session ->
            session.transaction { transactionalSession ->
                TransactionalEgenAnsattDao(transactionalSession).lagre(fødselsnummer, erEgenAnsatt, opprettet)
            }
        }
    }

    override fun erEgenAnsatt(fødselsnummer: String): Boolean? =
        sessionOf(dataSource).use { session ->
            session.transaction { transactionalSession ->
                TransactionalEgenAnsattDao(transactionalSession).erEgenAnsatt(fødselsnummer)
            }
        }
}
