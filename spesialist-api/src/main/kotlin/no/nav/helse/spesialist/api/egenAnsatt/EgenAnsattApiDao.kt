package no.nav.helse.spesialist.api.egenAnsatt

import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language

class EgenAnsattApiDao(private val dataSource: DataSource) {
    fun erEgenAnsatt(fødselsnummer: String): Boolean? {
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