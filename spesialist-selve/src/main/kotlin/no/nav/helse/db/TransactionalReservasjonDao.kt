package no.nav.helse.db

import kotliquery.Session
import kotliquery.queryOf
import org.intellij.lang.annotations.Language
import java.util.UUID

class TransactionalReservasjonDao(private val session: Session) : ReservasjonRepository {
    override fun reserverPerson(
        saksbehandlerOid: UUID,
        fødselsnummer: String,
    ) {
        @Language("PostgreSQL")
        val query = """
            INSERT INTO reserver_person(saksbehandler_ref, person_ref)
            SELECT :saksbehandler_ref, person.id
            FROM person
            WHERE person.fodselsnummer = :fodselsnummer
            ON CONFLICT (person_ref)
                DO UPDATE SET gyldig_til = current_date + time '23:59:59',
                              saksbehandler_ref = :saksbehandler_ref;
            """
        session.run(
            queryOf(
                query,
                mapOf(
                    "saksbehandler_ref" to saksbehandlerOid,
                    "fodselsnummer" to fødselsnummer.toLong(),
                ),
            ).asUpdate,
        )
    }

    override fun hentReservasjonFor(fødselsnummer: String): Reservasjon? {
        @Language("PostgreSQL")
        val query = """
            SELECT r.*, s.* FROM reserver_person r
            JOIN person p ON p.id = r.person_ref
            JOIN saksbehandler s ON r.saksbehandler_ref = s.oid
            WHERE p.fodselsnummer = :fnr AND r.gyldig_til > now();
            """
        return session.run(
            queryOf(
                query,
                mapOf(
                    "fnr" to fødselsnummer.toLong(),
                ),
            ).map { row ->
                Reservasjon(
                    SaksbehandlerFraDatabase(
                        oid = row.uuid("oid"),
                        navn = row.string("navn"),
                        epostadresse = row.string("epost"),
                        ident = row.string("ident"),
                    ),
                )
            }.asSingle,
        )
    }
}
