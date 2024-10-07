package no.nav.helse.db

import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.helse.modell.person.toFødselsnummer
import org.intellij.lang.annotations.Language
import java.util.UUID

class TransactionalMeldingDao(private val transactionalSession: TransactionalSession) : MeldingRepository {
    override fun finnFødselsnummer(meldingId: UUID): String {
        @Language("PostgreSQL")
        val statement = """SELECT fodselsnummer FROM hendelse WHERE id = ?"""
        return requireNotNull(
            transactionalSession.run(
                queryOf(statement, meldingId).map {
                    it.long("fodselsnummer").toFødselsnummer()
                }.asSingle,
            ),
        )
    }
}
