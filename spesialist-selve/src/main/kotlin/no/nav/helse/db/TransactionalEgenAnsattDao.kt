package no.nav.helse.db

import kotliquery.TransactionalSession
import kotliquery.queryOf
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import javax.naming.OperationNotSupportedException

class TransactionalEgenAnsattDao(private val transactionalSession: TransactionalSession) : EgenAnsattRepository {
    override fun erEgenAnsatt(fødselsnummer: String): Boolean? {
        @Language("PostgreSQL")
        val query =
            """
            SELECT er_egen_ansatt
            FROM egen_ansatt ea
                INNER JOIN person p on p.id = ea.person_ref
            WHERE p.fodselsnummer = :fodselsnummer
            """.trimIndent()
        return transactionalSession.run(
            queryOf(
                query,
                mapOf("fodselsnummer" to fødselsnummer.toLong()),
            )
                .map { it.boolean("er_egen_ansatt") }
                .asSingle,
        )
    }

    override fun lagre(
        fødselsnummer: String,
        erEgenAnsatt: Boolean,
        opprettet: LocalDateTime,
    ) {
        throw OperationNotSupportedException()
    }
}
