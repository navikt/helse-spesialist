package no.nav.helse.spesialist.db.dao.api

import no.nav.helse.db.api.VergemålApiDao
import no.nav.helse.spesialist.db.HelseDao
import javax.sql.DataSource

class PgVergemålApiDao internal constructor(dataSource: DataSource) : HelseDao(dataSource), VergemålApiDao {
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
