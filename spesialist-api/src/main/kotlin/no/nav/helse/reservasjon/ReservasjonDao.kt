package no.nav.helse.reservasjon

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

class ReservasjonDao(private val dataSource: DataSource) {
    fun reserverPerson(saksbehandlerOid: UUID, fødselsnummer: String) {
        @Language("PostgreSQL")
        val query = """INSERT INTO reserver_person(saksbehandler_ref, person_ref)
SELECT :saksbehandler_ref, person.id
FROM person
WHERE person.fodselsnummer = :fodselsnummer;"""
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query, mapOf(
                        "saksbehandler_ref" to saksbehandlerOid,
                        "fodselsnummer" to fødselsnummer.toLong()
                    )
                ).asUpdate
            )
        }
    }

    fun hentReservasjonFor(fødselsnummer: String) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query =
            """
                SELECT r.* FROM reserver_person r
                    JOIN person p ON p.id = r.person_ref
                WHERE p.fodselsnummer = ? AND r.gyldig_til > now();
            """
        session.run(
            queryOf(query, fødselsnummer.toLong())
                .map {
                    UUID.fromString(it.string("saksbehandler_ref")) to
                        it.localDateTime("gyldig_til")
                }
                .asSingle
        )
    }

    fun hentReservasjonFor(personReferanse: Long): UUID? {
        @Language("PostgreSQL")
        val query =
            "SELECT * FROM reserver_person WHERE person_ref=:person_ref ORDER BY gyldig_til DESC;"
        return sessionOf(dataSource).use {
            it.run(queryOf(query, mapOf("person_ref" to personReferanse)).map { row ->
                UUID.fromString(row.string("saksbehandler_ref"))
            }.asSingle)
        }
    }

    fun slettReservasjon(saksbehandlerOid: UUID, personReferanse: Long) {
        @Language("PostgreSQL")
        val query = "DELETE FROM reserver_person WHERE person_ref=:person_ref AND saksbehandler_ref=:saksbehandler_ref;"
        sessionOf(dataSource).use {
            it.run(
                queryOf(
                    query,
                    mapOf("saksbehandler_ref" to saksbehandlerOid, "person_ref" to personReferanse)
                ).asUpdate
            )
        }
    }
}
