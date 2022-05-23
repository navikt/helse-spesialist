package no.nav.helse.reservasjon

import no.nav.helse.HelseDao
import java.util.*
import javax.sql.DataSource
import org.intellij.lang.annotations.Language

class ReservasjonDao(dataSource: DataSource) : HelseDao(dataSource) {
    fun reserverPerson(saksbehandlerOid: UUID, fødselsnummer: String) =
        """ INSERT INTO reserver_person(saksbehandler_ref, person_ref)
            SELECT :saksbehandler_ref, person.id
            FROM person
            WHERE person.fodselsnummer = :fodselsnummer;"""
            .update(mapOf("saksbehandler_ref" to saksbehandlerOid, "fodselsnummer" to fødselsnummer.toLong()))

    fun hentReservasjonFor(fødselsnummer: String) =
        """ SELECT r.* FROM reserver_person r
                JOIN person p ON p.id = r.person_ref
            WHERE p.fodselsnummer = :fnr AND r.gyldig_til > now(); """
            .single(mapOf("fnr" to fødselsnummer.toLong())) {
                UUID.fromString(it.string("saksbehandler_ref")) to it.localDateTime(
                    "gyldig_til"
                )
            }

    fun hentReservasjonFor(personReferanse: Long): UUID? =
        """SELECT * FROM reserver_person WHERE person_ref=:person_ref ORDER BY gyldig_til DESC;"""
            .single(mapOf("person_ref" to personReferanse)) { row -> UUID.fromString(row.string("saksbehandler_ref")) }

    fun slettReservasjon(saksbehandlerOid: UUID, personReferanse: Long) =
        """DELETE FROM reserver_person WHERE person_ref=:person_ref AND saksbehandler_ref=:saksbehandler_ref;"""
            .update(mapOf("saksbehandler_ref" to saksbehandlerOid, "person_ref" to personReferanse))

    fun slettReservasjon(fødselsnummer: String): Int {
        @Language("PostgreSQL")
        val query =
            """
                DELETE FROM reserver_person rp 
                WHERE rp.person_ref IN (SELECT id from person WHERE fodselsnummer = :fnr);
            """
        return query.update(mapOf("fnr" to fødselsnummer.toLong()))
    }
}
