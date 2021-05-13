package no.nav.helse.person

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.vedtaksperiode.EnhetDto
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

class PersonApiDao(private val dataSource: DataSource) {
    fun finnEnhet(fødselsnummer: String) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = "SELECT id, navn from enhet WHERE id = (SELECT enhet_ref FROM person where fodselsnummer = ?);"
        requireNotNull(
            session.run(
                queryOf(query, fødselsnummer.toLong())
                    .map { row ->
                        EnhetDto(
                            row.string("id"),
                            row.string("navn")
                        )
                    }.asSingle
            )
        )
    }

    fun finnInfotrygdutbetalinger(fødselsnummer: String) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = """
            SELECT data FROM infotrygdutbetalinger
            WHERE id = (SELECT infotrygdutbetalinger_ref FROM person WHERE fodselsnummer = ?);
        """
        session.run(
            queryOf(query, fødselsnummer.toLong())
                .map { row -> row.string("data") }
                .asSingle
        )
    }
}
