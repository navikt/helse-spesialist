package no.nav.helse.modell.vergemal

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import javax.sql.DataSource

data class Vergemål(
    val harVergemål: Boolean,
    val harFremtidsfullmakter: Boolean,
    val harFullmakter: Boolean,
)

class VergemålDao(val dataSource: DataSource) {
    fun lagre(
        fødselsnummer: String,
        vergemål: Vergemål,
    ) {
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO vergemal (person_ref, har_vergemal, har_fremtidsfullmakter, har_fullmakter, vergemål_oppdatert, fullmakt_oppdatert)
            VALUES (
                (SELECT id FROM person WHERE fodselsnummer = :fodselsnummer),
                :har_vergemal,
                :har_fremtidsfullmakter,
                :har_fullmakter,
                :vergemal_oppdatert,
                :fullmakt_oppdatert
            )
            ON CONFLICT (person_ref)
            DO UPDATE SET
                har_vergemal = :har_vergemal,
                har_fremtidsfullmakter = :har_fremtidsfullmakter,
                har_fullmakter = :har_fullmakter,
                vergemål_oppdatert = :vergemal_oppdatert,
                fullmakt_oppdatert = :fullmakt_oppdatert
        """
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    statement,
                    mapOf(
                        "fodselsnummer" to fødselsnummer.toLong(),
                        "har_vergemal" to vergemål.harVergemål,
                        "har_fremtidsfullmakter" to vergemål.harFullmakter,
                        "har_fullmakter" to vergemål.harFullmakter,
                        "vergemal_oppdatert" to LocalDateTime.now(),
                        "fullmakt_oppdatert" to LocalDateTime.now(),
                    ),
                ).asExecute,
            )
        }
    }

    fun harVergemål(fødselsnummer: String): Boolean? {
        @Language("PostgreSQL")
        val query = """
            SELECT har_vergemal, har_fremtidsfullmakter, har_fullmakter
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
                    .map {
                        Vergemål(
                            it.boolean("har_vergemal"),
                            it.boolean("har_fremtidsfullmakter"),
                            it.boolean("har_fullmakter"),
                        ).harVergemål
                    }
                    .asSingle,
            )
        }
    }
}
