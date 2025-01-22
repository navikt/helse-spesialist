package no.nav.helse.db.api

import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.QueryRunner
import javax.sql.DataSource

class PgEgenAnsattApiDao(private val dataSource: DataSource) :
    QueryRunner by MedDataSource(dataSource),
    EgenAnsattApiDao {
    override fun erEgenAnsatt(fødselsnummer: String): Boolean? =
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
