package no.nav.helse.db

import no.nav.helse.HelseDao
import java.util.UUID
import javax.sql.DataSource

class ReservasjonDao(dataSource: DataSource) : HelseDao(dataSource), ReservasjonRepository {
    override fun reserverPerson(
        saksbehandlerOid: UUID,
        fødselsnummer: String,
    ) {
        asSQL(
            """
            INSERT INTO reserver_person(saksbehandler_ref, person_ref)
            SELECT :saksbehandler_ref, person.id
            FROM person
            WHERE person.fodselsnummer = :fodselsnummer
            ON CONFLICT (person_ref)
                DO UPDATE SET gyldig_til = current_date + time '23:59:59',
                              saksbehandler_ref = :saksbehandler_ref;
            """,
            mapOf(
                "saksbehandler_ref" to saksbehandlerOid,
                "fodselsnummer" to fødselsnummer.toLong(),
            ),
        ).update()
    }

    fun hentReservasjonFor(fødselsnummer: String): Reservasjon? =
        asSQL(
            """ SELECT r.*, s.* FROM reserver_person r
            JOIN person p ON p.id = r.person_ref
            JOIN saksbehandler s ON r.saksbehandler_ref = s.oid
            WHERE p.fodselsnummer = :fnr AND r.gyldig_til > now();
        """,
            mapOf("fnr" to fødselsnummer.toLong()),
        ).single {
            Reservasjon(
                SaksbehandlerFraDatabase(
                    oid = it.uuid("oid"),
                    navn = it.string("navn"),
                    epostadresse = it.string("epost"),
                    ident = it.string("ident"),
                ),
            )
        }
}

data class Reservasjon(
    val reservertTil: SaksbehandlerFraDatabase,
)
