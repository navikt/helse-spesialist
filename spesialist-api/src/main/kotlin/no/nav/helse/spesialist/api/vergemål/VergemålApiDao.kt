package no.nav.helse.spesialist.api.vergemål

import no.nav.helse.HelseDao
import javax.sql.DataSource

class VergemålApiDao(dataSource: DataSource) : HelseDao(dataSource) {
    fun harFullmakt(fødselsnummer: String): Boolean? =
        asSQL(
            """
                SELECT har_fremtidsfullmakter, har_fullmakter
                FROM vergemal v
                INNER JOIN person p on p.id = v.person_ref
                WHERE p.fodselsnummer = :fodselsnummer
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer
        ).single { row ->
            row.boolean("har_fremtidsfullmakter") || row.boolean("har_fullmakter")
        }
}