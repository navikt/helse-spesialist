package no.nav.helse.modell.arbeidsgiver

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import javax.sql.DataSource

internal class ArbeidsgiverDao(private val dataSource: DataSource) {
    internal fun findArbeidsgiverByOrgnummer(orgnummer: String) = sessionOf(dataSource).use {
        it.findArbeidsgiverByOrgnummer(orgnummer)
    }

    internal fun insertArbeidsgiver(orgnummer: String, navn: String, bransjer: List<String>) =
        sessionOf(dataSource, returnGeneratedKey = true).use {
            val navnRef = requireNotNull(it.insertArbeidsgivernavn(navn))
            val bransjerRef = requireNotNull(it.insertBransjer(bransjer))
            it.run(
                queryOf(
                    "INSERT INTO arbeidsgiver(orgnummer, navn_ref, bransjer_ref) VALUES(:orgnummer, :navnRef, :bransjerRef);",
                    mapOf(
                        "orgnummer" to orgnummer.toLong(),
                        "navnRef" to navnRef,
                        "bransjerRef" to bransjerRef
                    )
                )
                    .asUpdateAndReturnGeneratedKey
            )
        }

    internal fun findNavnSistOppdatert(orgnummer: String) = sessionOf(dataSource).use { session ->
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

    internal fun findBransjerSistOppdatert(orgnummer: String) = sessionOf(dataSource).use { session ->
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

    internal fun findArbeidsgiver(arbeidsgiverId: Long) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = """
            SELECT an.navn, a.orgnummer, ab.bransjer FROM arbeidsgiver AS a
                JOIN arbeidsgiver_navn AS an ON a.navn_ref = an.id
                LEFT JOIN arbeidsgiver_bransjer ab on a.bransjer_ref = ab.id
            WHERE a.id=?;
        """
        session.run(
            queryOf(query, arbeidsgiverId).map { row ->
                ArbeidsgiverDto(
                    organisasjonsnummer = row.string("orgnummer"),
                    navn = row.string("navn"),
                    bransjer = row.stringOrNull("bransjer")
                        ?.takeIf { it.isNotBlank() }
                        ?.let { objectMapper.readValue(it) }
                        ?: emptyList()
                )
            }.asSingle
        )
    }

    internal fun updateNavn(orgnummer: String, navn: String) = sessionOf(dataSource).use {
        it.run(
            queryOf(
                "UPDATE arbeidsgiver_navn SET navn=?, navn_oppdatert=? WHERE id=(SELECT navn_ref FROM arbeidsgiver WHERE orgnummer=?);",
                navn,
                LocalDateTime.now(),
                orgnummer.toLong()
            ).asUpdate
        )
    }

    internal fun updateBransjer(orgnummer: String, bransjer: List<String>) = sessionOf(dataSource).use {
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

    internal fun insertBransjer(orgnummer: String, bransjer: List<String>): Int =
        sessionOf(dataSource, returnGeneratedKey = true).use {
            @Language("PostgreSQL")
            val insertBransjeStatement =
                "INSERT INTO arbeidsgiver_bransjer (bransjer, oppdatert) VALUES (?, now());"

            @Language("PostgreSQL")
            val updateBransjeRefStatement =
                "UPDATE arbeidsgiver SET bransjer_ref=:bransje_ref WHERE orgnummer=:orgnummer;"

            it.transaction { transaction ->
                val bransjeRef =
                    transaction.run(
                        queryOf(
                            insertBransjeStatement,
                            objectMapper.writeValueAsString(bransjer)
                        ).asUpdateAndReturnGeneratedKey
                    )
                transaction.run(
                    queryOf(
                        updateBransjeRefStatement,
                        mapOf(
                            "bransje_ref" to bransjeRef,
                            "orgnummer" to orgnummer.toLong()
                        )
                    ).asUpdate
                )
            }
        }
}

internal fun Session.findArbeidsgiverByOrgnummer(orgnummer: String): Long? = this.run(
    queryOf("SELECT id FROM arbeidsgiver WHERE orgnummer=?;", orgnummer.toLong())
        .map { it.long("id") }
        .asSingle
)

private fun Session.insertArbeidsgivernavn(navn: String): Long? = run(
    queryOf(
        "INSERT INTO arbeidsgiver_navn(navn, navn_oppdatert) VALUES(?, ?);",
        navn,
        LocalDateTime.now()
    )
        .asUpdateAndReturnGeneratedKey
)

private fun Session.insertBransjer(bransjer: List<String>) = run(
    queryOf(
        "INSERT INTO arbeidsgiver_bransjer(bransjer, oppdatert) VALUES(:bransjer, :oppdatert);",
        mapOf(
            "bransjer" to objectMapper.writeValueAsString(bransjer),
            "oppdatert" to LocalDateTime.now()
        )
    )
        .asUpdateAndReturnGeneratedKey
)
