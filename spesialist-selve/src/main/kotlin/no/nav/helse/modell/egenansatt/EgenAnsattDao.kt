package no.nav.helse.modell.egenansatt

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import javax.sql.DataSource

class EgenAnsattDao(private val dataSource: DataSource) {
    internal fun lagre(fødselsnummer: String, erEgenAnsatt: Boolean, opprettet: LocalDateTime) {
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO egen_ansatt (person_ref, er_egen_ansatt, opprettet)
            VALUES (
                (SELECT id FROM person WHERE fodselsnummer = :fodselsnummer),
                :er_egen_ansatt,
                :opprettet
            )
            ON CONFLICT (person_ref) DO UPDATE SET er_egen_ansatt = :er_egen_ansatt, opprettet = :opprettet
        """
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    statement,
                    mapOf(
                        "fodselsnummer" to fødselsnummer.toLong(),
                        "er_egen_ansatt" to erEgenAnsatt,
                        "opprettet" to opprettet
                    )
                ).asExecute
            )
        }
    }

    internal fun erEgenAnsatt(fødselsnummer: String): Boolean? {
        @Language("PostgreSQL")
        val query = """
            SELECT er_egen_ansatt
                FROM egen_ansatt ea
                    INNER JOIN person p on p.id = ea.person_ref
                WHERE p.fodselsnummer = :fodselsnummer
            """
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "fodselsnummer" to fødselsnummer.toLong()
                    )
                )
                    .map { it.boolean("er_egen_ansatt") }
                    .asSingle
            )
        }
    }
}
