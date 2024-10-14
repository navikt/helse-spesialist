package no.nav.helse.db

import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.helse.modell.vergemal.VergemålOgFremtidsfullmakt
import org.intellij.lang.annotations.Language
import javax.naming.OperationNotSupportedException

class TransactionalVergemålDao(private val transactionalSession: TransactionalSession) : VergemålRepository {
    override fun lagre(
        fødselsnummer: String,
        vergemålOgFremtidsfullmakt: VergemålOgFremtidsfullmakt,
        fullmakt: Boolean,
    ) {
        throw OperationNotSupportedException()
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
        return transactionalSession.run(
            queryOf(
                query,
                mapOf("fodselsnummer" to fødselsnummer.toLong()),
            )
                .map { it.boolean("har_vergemal") }
                .asSingle,
        )
    }
}
