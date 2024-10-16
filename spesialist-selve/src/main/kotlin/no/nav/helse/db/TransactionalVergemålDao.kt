package no.nav.helse.db

import kotliquery.Session
import kotliquery.queryOf
import no.nav.helse.modell.vergemal.VergemålOgFremtidsfullmakt
import org.intellij.lang.annotations.Language

class TransactionalVergemålDao(private val session: Session) : VergemålRepository {
    override fun lagre(
        fødselsnummer: String,
        vergemålOgFremtidsfullmakt: VergemålOgFremtidsfullmakt,
        fullmakt: Boolean,
    ) {
        throw UnsupportedOperationException()
    }

    override fun harVergemål(fødselsnummer: String): Boolean? {
        @Language("PostgreSQL")
        val query =
            """
            SELECT har_vergemal
            FROM vergemal v
                INNER JOIN person p on p.id = v.person_ref
            WHERE p.fodselsnummer = :fodselsnummer
            """.trimIndent()
        return session.run(
            queryOf(
                query,
                mapOf("fodselsnummer" to fødselsnummer.toLong()),
            )
                .map { it.boolean("har_vergemal") }
                .asSingle,
        )
    }
}
