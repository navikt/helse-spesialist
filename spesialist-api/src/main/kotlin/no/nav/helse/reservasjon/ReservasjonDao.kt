package no.nav.helse.reservasjon

import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.HelseDao

class ReservasjonDao(dataSource: DataSource) : HelseDao(dataSource) {
    fun reserverPerson(saksbehandlerOid: UUID, fødselsnummer: String) =
        """ INSERT INTO reserver_person(saksbehandler_ref, person_ref)
            SELECT :saksbehandler_ref, person.id
            FROM person
            WHERE person.fodselsnummer = :fodselsnummer;"""
            .update(mapOf("saksbehandler_ref" to saksbehandlerOid, "fodselsnummer" to fødselsnummer.toLong()))

    fun hentReservasjonFor(fødselsnummer: String): UUID? =
        """ SELECT r.* FROM reserver_person r
                JOIN person p ON p.id = r.person_ref
            WHERE p.fodselsnummer = :fnr AND r.gyldig_til > now(); """
            .single(mapOf("fnr" to fødselsnummer.toLong())) {
                UUID.fromString(it.string("saksbehandler_ref"))
            }
}
