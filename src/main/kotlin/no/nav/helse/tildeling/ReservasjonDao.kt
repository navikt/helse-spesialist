package no.nav.helse.tildeling

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

internal class ReservasjonDao(private val dataSource: DataSource) {
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
        val query = """
SELECT r.*
FROM reserver_person r
         JOIN person p ON p.id = r.person_ref
WHERE p.fodselsnummer = :fodselsnummer
  AND r.gyldig_til > now();
"""
        session.run(
            queryOf(
                query,
                mapOf("fodselsnummer" to fødselsnummer.toLong())
            )
                .map(::tilReservasjon)
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

    private fun tilReservasjon(row: Row): ReservasjonDto = ReservasjonDto(
        UUID.fromString(row.string("saksbehandler_ref")),
        row.localDateTime("gyldig_til")
    )
}


internal fun Session.hentReservasjonFor(fødselsnummer: String) = run(
        queryOf(
            """
        SELECT r.*
        FROM reserver_person r
                 JOIN person p ON p.id = r.person_ref
        WHERE p.fodselsnummer = :fodselsnummer
          AND r.gyldig_til > now();
        """,
            mapOf("fodselsnummer" to fødselsnummer.toLong())
        )
            .map(::tilReservasjon)
            .asSingle
    )

private fun tilReservasjon(row: Row): ReservasjonDto = ReservasjonDto(
    UUID.fromString(row.string("saksbehandler_ref")),
    row.localDateTime("gyldig_til")
)

data class ReservasjonDto(
    val saksbehandlerOid: UUID,
    val gyldigTil: LocalDateTime
)
