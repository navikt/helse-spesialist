package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.HelseDao.Companion.asSQL
import java.util.UUID
import javax.sql.DataSource

class ReservasjonDao(queryRunner: QueryRunner) : ReservasjonRepository, QueryRunner by queryRunner {
    constructor(session: Session) : this(MedSession(session))
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    override fun reserverPerson(
        saksbehandlerOid: UUID,
        fødselsnummer: String,
    ) {
        asSQL(
            """
            INSERT INTO reserver_person(saksbehandler_ref, person_ref)
            SELECT :saksbehandler_ref, person.id
            FROM person
            WHERE person.fødselsnummer = :foedselsnummer
            ON CONFLICT (person_ref)
                DO UPDATE SET gyldig_til = current_date + time '23:59:59',
                              saksbehandler_ref = :saksbehandler_ref;
            """.trimIndent(),
            "saksbehandler_ref" to saksbehandlerOid,
            "foedselsnummer" to fødselsnummer,
        ).update()
    }

    override fun hentReservasjonFor(fødselsnummer: String): Reservasjon? =
        asSQL(
            """
            SELECT r.*, s.* FROM reserver_person r
            JOIN person p ON p.id = r.person_ref
            JOIN saksbehandler s ON r.saksbehandler_ref = s.oid
            WHERE p.fødselsnummer = :foedselsnummer AND r.gyldig_til > now();
            """.trimIndent(),
            "foedselsnummer" to fødselsnummer,
        ).singleOrNull { row ->
            Reservasjon(
                SaksbehandlerFraDatabase(
                    oid = row.uuid("oid"),
                    navn = row.string("navn"),
                    epostadresse = row.string("epost"),
                    ident = row.string("ident"),
                ),
            )
        }
}

data class Reservasjon(
    val reservertTil: SaksbehandlerFraDatabase,
)
