package no.nav.helse.arbeidsgiver

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import javax.sql.DataSource

class ArbeidsgiverDao(private val dataSource: DataSource) {

    fun findArbeidsgiverByOrgnummer(orgnummer: String) = sessionOf(dataSource).use { session ->
        session.run(
            queryOf("SELECT id FROM arbeidsgiver WHERE orgnummer=?;", orgnummer.toLong())
                .map { it.long("id") }
                .asSingle
        )
    }

    fun insertArbeidsgiver(orgnummer: String, navn: String, bransjer: List<String>) =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            val navnRef = requireNotNull(
                session.run(
                    queryOf(
                        "INSERT INTO arbeidsgiver_navn(navn, navn_oppdatert) VALUES(?, ?);",
                        navn,
                        LocalDateTime.now()
                    ).asUpdateAndReturnGeneratedKey
                )
            )
            val bransjerRef = requireNotNull(
                session.run(
                    queryOf(
                        "INSERT INTO arbeidsgiver_bransjer(bransjer, oppdatert) VALUES(:bransjer, :oppdatert);",
                        mapOf(
                            "bransjer" to objectMapper.writeValueAsString(bransjer),
                            "oppdatert" to LocalDateTime.now()
                        )
                    ).asUpdateAndReturnGeneratedKey
                )
            )
            session.run(
                queryOf(
                    "INSERT INTO arbeidsgiver(orgnummer, navn_ref, bransjer_ref) VALUES(:orgnummer, :navnRef, :bransjerRef);",
                    mapOf(
                        "orgnummer" to orgnummer.toLong(),
                        "navnRef" to navnRef,
                        "bransjerRef" to bransjerRef
                    )
                ).asUpdateAndReturnGeneratedKey
            )
        }

    fun findNavnSistOppdatert(orgnummer: String) = sessionOf(dataSource).use { session ->
        requireNotNull(
            session.run(
                queryOf(
                    "SELECT navn_oppdatert FROM arbeidsgiver_navn WHERE id=(SELECT navn_ref FROM arbeidsgiver WHERE orgnummer=?);",
                    orgnummer.toLong()
                ).map {
                    it.localDate("navn_oppdatert")
                }.asSingle
            )
        )
    }

    fun findBransjerSistOppdatert(orgnummer: String) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement =
            "SELECT oppdatert FROM arbeidsgiver_bransjer WHERE id=(SELECT bransjer_ref FROM arbeidsgiver WHERE orgnummer=:orgnummer);"
        session.run(
            queryOf(
                statement,
                mapOf("orgnummer" to orgnummer.toLong())
            ).map {
                it.localDate("oppdatert")
            }.asSingle
        )
    }

    fun finnBransjer(orgnummer: String) = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val query = """
            SELECT ab.bransjer FROM arbeidsgiver a
                LEFT JOIN arbeidsgiver_bransjer ab on a.bransjer_ref = ab.id
            WHERE a.orgnummer=?;
        """
        session.run(
            queryOf(query, orgnummer.toLong()).map { row ->
                row.stringOrNull("bransjer")
                    ?.let { objectMapper.readValue<List<String>>(it) }
                    ?.filter { it.isNotBlank() }

            }.asSingle
        ) ?: emptyList()
    }

    fun finnNavn(orgnummer: String) = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val query = """
            SELECT an.navn FROM arbeidsgiver a
                JOIN arbeidsgiver_navn an ON a.navn_ref = an.id
            WHERE a.orgnummer=?;
        """
        session.run(
            queryOf(query, orgnummer.toLong()).map { row ->
                row.string("navn")
            }.asSingle
        )
    }

    fun updateNavn(orgnummer: String, navn: String) = sessionOf(dataSource).use {
        it.run(
            queryOf(
                "UPDATE arbeidsgiver_navn SET navn=?, navn_oppdatert=? WHERE id=(SELECT navn_ref FROM arbeidsgiver WHERE orgnummer=?);",
                navn,
                LocalDateTime.now(),
                orgnummer.toLong()
            ).asUpdate
        )
    }

    fun updateBransjer(orgnummer: String, bransjer: List<String>) = sessionOf(dataSource).use {
        @Language("PostgreSQL")
        val statement = """
            UPDATE arbeidsgiver_bransjer
            SET bransjer=:bransjer, oppdatert=:oppdatert
            WHERE id=(SELECT bransjer_ref FROM arbeidsgiver WHERE orgnummer=:orgnummer);
        """

        it.run(
            queryOf(
                statement,
                mapOf(
                    "bransjer" to objectMapper.writeValueAsString(bransjer),
                    "oppdatert" to LocalDateTime.now(),
                    "orgnummer" to orgnummer.toLong()
                )
            ).asUpdate
        )
    }

}

