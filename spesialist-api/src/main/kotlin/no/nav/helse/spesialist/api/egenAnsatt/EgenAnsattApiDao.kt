package no.nav.helse.spesialist.api.egenAnsatt

import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.QueryRunner
import javax.sql.DataSource

class EgenAnsattApiDao(private val dataSource: DataSource) : QueryRunner by MedDataSource(dataSource) {
    fun erEgenAnsatt(fødselsnummer: String): Boolean? =
        asSQL(
            """
            SELECT er_egen_ansatt
                FROM egen_ansatt ea
                    INNER JOIN person p on p.id = ea.person_ref
                WHERE p.fødselsnummer = :fodselsnummer
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
        ).singleOrNull { it.boolean("er_egen_ansatt") }
}
