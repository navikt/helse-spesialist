package no.nav.helse.modell.vergemal

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import javax.sql.DataSource

data class VergemålOgFremtidsfullmakt(
    val harVergemål: Boolean,
    val harFremtidsfullmakter: Boolean,
)

class VergemålDao(val dataSource: DataSource) {
    fun lagre(
        fødselsnummer: String,
        vergemålOgFremtidsfullmakt: VergemålOgFremtidsfullmakt,
        fullmakt: Boolean,
    ) {
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO vergemal (person_ref, har_vergemal, har_fremtidsfullmakter, har_fullmakter, vergemål_oppdatert, fullmakt_oppdatert)
            VALUES (
                (SELECT id FROM person WHERE fodselsnummer = :fodselsnummer),
                :har_vergemal,
                :har_fremtidsfullmakter,
                :har_fullmakter,
                :oppdatert,
                :oppdatert
            )
            ON CONFLICT (person_ref)
            DO UPDATE SET
                har_vergemal = :har_vergemal,
                har_fremtidsfullmakter = :har_fremtidsfullmakter,
                har_fullmakter = :har_fullmakter,
                vergemål_oppdatert = :oppdatert,
                fullmakt_oppdatert = :oppdatert
        """
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    statement,
                    mapOf(
                        "fodselsnummer" to fødselsnummer.toLong(),
                        "har_vergemal" to vergemålOgFremtidsfullmakt.harVergemål,
                        "har_fremtidsfullmakter" to vergemålOgFremtidsfullmakt.harFremtidsfullmakter,
                        "har_fullmakter" to fullmakt,
                        "oppdatert" to LocalDateTime.now(),
                    ),
                ).asExecute,
            )
        }
    }

    fun harVergemål(fødselsnummer: String): Boolean? {
        @Language("PostgreSQL")
        val query = """
            SELECT har_vergemal
                FROM vergemal v
                    INNER JOIN person p on p.id = v.person_ref
                WHERE p.fodselsnummer = :fodselsnummer
            """
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "fodselsnummer" to fødselsnummer.toLong(),
                    ),
                )
                    .map { it.boolean("har_vergemal") }
                    .asSingle,
            )
        }
    }

    fun harFullmakt(fødselsnummer: String): Boolean? {
        @Language("PostgreSQL")
        val query = """
            SELECT har_fremtidsfullmakter, har_fullmakter
                FROM vergemal v
                    INNER JOIN person p on p.id = v.person_ref
                WHERE p.fodselsnummer = :fodselsnummer
            """
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "fodselsnummer" to fødselsnummer.toLong(),
                    ),
                )
                    .map { row ->
                        row.boolean("har_fremtidsfullmakter") || row.boolean("har_fullmakter")
                    }.asSingle,
            )
        }
    }
}
