package no.nav.helse.db

import kotliquery.sessionOf
import no.nav.helse.HelseDao
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak
import java.time.LocalDateTime
import javax.sql.DataSource

class StansAutomatiskBehandlingDao(private val dataSource: DataSource) :
    HelseDao(dataSource),
    StansAutomatiskBehandlingRepository {
    override fun lagreFraISyfo(
        fødselsnummer: String,
        status: String,
        årsaker: Set<StoppknappÅrsak>,
        opprettet: LocalDateTime,
        originalMelding: String?,
        kilde: String,
    ) {
        sessionOf(dataSource).use { session ->
            TransactionalStansAutomatiskBehandlingDao(session).lagreFraISyfo(
                fødselsnummer,
                status,
                årsaker,
                opprettet,
                originalMelding,
                kilde,
            )
        }
    }

    override fun lagreFraSpeil(fødselsnummer: String) {
        sessionOf(dataSource).use { session ->
            TransactionalStansAutomatiskBehandlingDao(session).lagreFraSpeil(fødselsnummer)
        }
    }

    override fun hentFor(fødselsnummer: String) =
        sessionOf(dataSource).use { session ->
            TransactionalStansAutomatiskBehandlingDao(session).hentFor(fødselsnummer)
        }
}

fun <T> Iterable<T>.somDbArray() = joinToString(prefix = "{", postfix = "}")
