package no.nav.helse.db

import kotliquery.Session
import kotliquery.queryOf
import org.intellij.lang.annotations.Language
import java.util.UUID

class TransactionalSaksbehandlerDao(
    private val session: Session,
) : SaksbehandlerRepository {
    override fun finnSaksbehandler(oid: UUID): SaksbehandlerFraDatabase? {
        @Language("PostgreSQL")
        val statement = " SELECT * FROM saksbehandler WHERE oid = :oid LIMIT 1; "

        return session.run(
            queryOf(
                statement,
                mapOf("oid" to oid),
            )
                .map { row ->
                    SaksbehandlerFraDatabase(
                        epostadresse = row.string("epost"),
                        oid = row.uuid("oid"),
                        navn = row.string("navn"),
                        ident = row.string("ident"),
                    )
                }.asSingle,
        )
    }
}
