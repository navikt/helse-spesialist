package no.nav.helse.tildeling

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

internal class ReservasjonDao(private val dataSource: DataSource) {
    fun reserverPerson(saksbehandlerOid: UUID, fødselsnummer: String) {
        @Language("PostgreSQL")
        val query = """INSERT INTO reserver_person(saksbehandler_ref, person_ref)
SELECT :saksbehandler_ref, person.id
FROM person
WHERE person.fodselsnummer = :fodselsnummer;"""
        sessionOf(dataSource).run(
            queryOf(
                query, mapOf(
                    "saksbehandler_ref" to saksbehandlerOid,
                    "fodselsnummer" to fødselsnummer.toLong()
                )
            ).asUpdate
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
