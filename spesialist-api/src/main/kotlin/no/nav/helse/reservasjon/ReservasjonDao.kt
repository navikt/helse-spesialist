package no.nav.helse.reservasjon

import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.HelseDao
import org.intellij.lang.annotations.Language

class ReservasjonDao(dataSource: DataSource) : HelseDao(dataSource) {
    fun reserverPerson(saksbehandlerOid: UUID, fødselsnummer: String) {
        @Language("PostgreSQL")
        val insertQuery = """
                INSERT INTO reserver_person(saksbehandler_ref, person_ref)
                SELECT :saksbehandler_ref, person.id
                FROM person
                WHERE person.fodselsnummer = :fodselsnummer
            """

        @Language("PostgreSQL")
        val updateQuery = """
                UPDATE reserver_person
                SET gyldig_til=now()
                WHERE saksbehandler_ref = :saksbehandler_ref and person_ref = (
                    SELECT id FROM person WHERE person.fodselsnummer = :fodselsnummer
                )
            """
        (if (hentReservasjonFor(fødselsnummer) == null) insertQuery else updateQuery).update(
            mapOf("saksbehandler_ref" to saksbehandlerOid, "fodselsnummer" to fødselsnummer.toLong())
        )
    }

    fun hentReservasjonFor(fødselsnummer: String) =
        """ SELECT r.* FROM reserver_person r
                JOIN person p ON p.id = r.person_ref
            WHERE p.fodselsnummer = :fnr AND r.gyldig_til > now(); """
            .single(mapOf("fnr" to fødselsnummer.toLong())) {
                UUID.fromString(it.string("saksbehandler_ref")) to it.localDateTime("gyldig_til")
            }

    fun hentReservasjonFor(personReferanse: Long): UUID? =
        """SELECT * FROM reserver_person WHERE person_ref=:person_ref ORDER BY gyldig_til DESC;"""
            .single(mapOf("person_ref" to personReferanse)) { row ->
                UUID.fromString(row.string("saksbehandler_ref"))
            }

    fun slettReservasjon(saksbehandlerOid: UUID, personReferanse: Long) =
        """DELETE FROM reserver_person WHERE person_ref=:person_ref AND saksbehandler_ref=:saksbehandler_ref;"""
            .update(mapOf("saksbehandler_ref" to saksbehandlerOid, "person_ref" to personReferanse))
}
