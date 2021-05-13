package no.nav.helse.arbeidsgiver

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

class ArbeidsgiverApiDao(private val dataSource: DataSource) {

    fun finnBransjer(orgnummer: String) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = """
            SELECT ab.bransjer FROM arbeidsgiver a
                LEFT JOIN arbeidsgiver_bransjer ab on a.bransjer_ref = ab.id
            WHERE a.orgnummer=?;
        """
        session.run(
            queryOf(query, orgnummer.toLong()).map { row ->
                row.stringOrNull("bransjer")
                    ?.let { objectMapper.readValue<List<String>>(it) }
                    ?.filter { it.isNotBlank() }

            }.asSingle
        ) ?: emptyList()
    }

    fun finnNavn(orgnummer: String) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = """
            SELECT an.navn FROM arbeidsgiver a
                JOIN arbeidsgiver_navn an ON a.navn_ref = an.id
            WHERE a.orgnummer=?;
        """
        session.run(
            queryOf(query, orgnummer.toLong()).map { row ->
                row.string("navn")
            }.asSingle
        )
    }
}

