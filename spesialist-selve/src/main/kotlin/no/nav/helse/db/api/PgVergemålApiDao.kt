package no.nav.helse.db.api

import no.nav.helse.HelseDao
import javax.sql.DataSource

class PgVergemålApiDao(dataSource: DataSource) : HelseDao(dataSource), VergemålApiDao {
    override fun harFullmakt(fødselsnummer: String): Boolean? =
        asSQL(
            """
            SELECT har_fremtidsfullmakter, har_fullmakter
            FROM vergemal v
            INNER JOIN person p on p.id = v.person_ref
            WHERE p.fødselsnummer = :fodselsnummer
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
        ).single { row ->
            row.boolean("har_fremtidsfullmakter") || row.boolean("har_fullmakter")
        }
}
