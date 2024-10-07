package no.nav.helse.db

import kotliquery.sessionOf
import no.nav.helse.HelseDao
import java.util.UUID
import javax.sql.DataSource

class ReservasjonDao(private val dataSource: DataSource) : HelseDao(dataSource), ReservasjonRepository {
    override fun reserverPerson(
        saksbehandlerOid: UUID,
        fødselsnummer: String,
    ) {
        sessionOf(dataSource).use { session ->
            session.transaction { transaction ->
                TransactionalReservasjonDao(transaction).reserverPerson(saksbehandlerOid, fødselsnummer)
            }
        }
    }

    override fun hentReservasjonFor(fødselsnummer: String): Reservasjon? =
        sessionOf(dataSource).use { session ->
            session.transaction { transaction ->
                TransactionalReservasjonDao(transaction).hentReservasjonFor(fødselsnummer)
            }
        }
}

data class Reservasjon(
    val reservertTil: SaksbehandlerFraDatabase,
)
