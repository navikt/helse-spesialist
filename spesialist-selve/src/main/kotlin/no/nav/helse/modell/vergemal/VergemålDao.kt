package no.nav.helse.modell.vergemal

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.db.TransactionalVergemålDao
import no.nav.helse.db.VergemålRepository
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

data class VergemålOgFremtidsfullmakt(
    val harVergemål: Boolean,
    val harFremtidsfullmakter: Boolean,
)

class VergemålDao(val dataSource: DataSource) : VergemålRepository {
    override fun lagre(
        fødselsnummer: String,
        vergemålOgFremtidsfullmakt: VergemålOgFremtidsfullmakt,
        fullmakt: Boolean,
    ) = sessionOf(dataSource).use {
        TransactionalVergemålDao(it).lagre(fødselsnummer, vergemålOgFremtidsfullmakt, fullmakt)
    }

    override fun harVergemål(fødselsnummer: String): Boolean? {
        return sessionOf(dataSource).use { session ->
            TransactionalVergemålDao(session).harVergemål(fødselsnummer)
        }
    }

    fun harFullmakt(fødselsnummer: String): Boolean? {
        @Language("PostgreSQL")
        val query = """
            SELECT har_fremtidsfullmakter, har_fullmakter
                FROM vergemal v
                    INNER JOIN person p on p.id = v.person_ref
                WHERE p.fodselsnummer = :fodselsnummer
            """
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "fodselsnummer" to fødselsnummer.toLong(),
                    ),
                )
                    .map { row ->
                        row.boolean("har_fremtidsfullmakter") || row.boolean("har_fullmakter")
                    }.asSingle,
            )
        }
    }
}
