package no.nav.helse.modell.arbeidsgiver

import java.time.LocalDateTime
import javax.sql.DataSource
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language

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

    fun insertArbeidsgiver(orgnummer: String) =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            session.run(
                queryOf(
                    "INSERT INTO arbeidsgiver(orgnummer) VALUES(:orgnummer);",
                    mapOf(
                        "orgnummer" to orgnummer.toLong()
                    )
                ).asUpdateAndReturnGeneratedKey
            )
        }

    fun findNavnSistOppdatert(orgnummer: String) = sessionOf(dataSource).use { session ->
        session.run(
            queryOf(
                "SELECT navn_oppdatert FROM arbeidsgiver_navn WHERE id=(SELECT navn_ref FROM arbeidsgiver WHERE orgnummer=?);",
                orgnummer.toLong()
            ).map {
                it.localDate("navn_oppdatert")
            }.asSingle
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

    fun updateOrInsertNavn(orgnummer: String, navn: String) = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        session.transaction { transaction ->
            transaction.finnArbeidsgiverNavnRef(orgnummer)
                ?.also { transaction.oppdaterArbeidsgivernavn(it, navn) }
                ?: transaction.opprettArbeidsgivernavn(orgnummer, navn)
        }
    }

    private fun TransactionalSession.finnArbeidsgiverNavnRef(orgnummer: String): Long? {
        @Language("PostgreSQL")
        val query = "SELECT navn_ref FROM arbeidsgiver WHERE orgnummer=?"

        return run(queryOf(query, orgnummer.toLong()).map { it.longOrNull("navn_ref") }.asSingle)
    }

    private fun TransactionalSession.oppdaterArbeidsgivernavn(arbeidsgivernavnRef: Long, navn: String) {
        @Language("PostgreSQL")
        val query = "UPDATE arbeidsgiver_navn SET navn=:navn, navn_oppdatert=now() WHERE id=:arbeidsgivernavnRef"

        run(queryOf(query, mapOf("navn" to navn, "arbeidsgivernavnRef" to arbeidsgivernavnRef)).asUpdate)
    }

    private fun TransactionalSession.opprettArbeidsgivernavn(orgnummer: String, navn: String) {
        @Language("PostgreSQL")
        val query = "INSERT INTO arbeidsgiver_navn (navn, navn_oppdatert) VALUES (?, now())"

        val arbeidsgivernavnId = requireNotNull(run(queryOf(query, navn).asUpdateAndReturnGeneratedKey))
        upsertNavnRef(arbeidsgivernavnId, orgnummer)
    }

    private fun TransactionalSession.upsertNavnRef(arbeidsgivernavnRef: Long, orgnummer: String) {
        @Language("PostgreSQL")
        val query = "INSERT INTO arbeidsgiver (orgnummer, navn_ref) VALUES(:orgnummer, :arbeidsgivernavnRef) ON CONFLICT(orgnummer) DO UPDATE SET navn_ref=:arbeidsgivernavnRef"

        run(queryOf(query, mapOf("arbeidsgivernavnRef" to arbeidsgivernavnRef, "orgnummer" to orgnummer.toLong())).asUpdate)
    }

    fun updateOrInsertBransjer(orgnummer: String, bransjer: List<String>) = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        session.transaction { transaction ->
            transaction.finnArbeidsgiverbransjerRef(orgnummer)
                ?.also { transaction.oppdaterArbeidsgiverbransjer(it, bransjer) }
                ?: transaction.opprettArbeidsgiverbransjer(orgnummer, bransjer)
        }
    }

    private fun TransactionalSession.finnArbeidsgiverbransjerRef(orgnummer: String): Long? {
        @Language("PostgreSQL")
        val query = "SELECT bransjer_ref FROM arbeidsgiver WHERE orgnummer=?"

        return run(queryOf(query, orgnummer.toLong()).map { it.longOrNull("bransjer_ref") }.asSingle)
    }

    private fun TransactionalSession.oppdaterArbeidsgiverbransjer(bransjerRef: Long, bransjer: List<String>) {
        @Language("PostgreSQL")
        val query = "UPDATE arbeidsgiver_bransjer SET bransjer=:bransjer, oppdatert=now() WHERE id=:bransjerRef"

        run(queryOf(query, mapOf("bransjer" to objectMapper.writeValueAsString(bransjer), "bransjerRef" to bransjerRef)).asUpdate)
    }

    private fun TransactionalSession.opprettArbeidsgiverbransjer(orgnummer: String, bransjer: List<String>) {
        @Language("PostgreSQL")
        val query = "INSERT INTO arbeidsgiver_bransjer (bransjer, oppdatert) VALUES (?, now())"

        val bransjerId = requireNotNull(run(queryOf(query, objectMapper.writeValueAsString(bransjer)).asUpdateAndReturnGeneratedKey))
        upsertBransjerRef(bransjerId, orgnummer)
    }

    private fun TransactionalSession.upsertBransjerRef(bransjerRef: Long, orgnummer: String) {
        @Language("PostgreSQL")
        val query = "INSERT INTO arbeidsgiver (orgnummer, bransjer_ref) VALUES(:orgnummer, :bransjerRef) ON CONFLICT(orgnummer) DO UPDATE SET bransjer_ref=:bransjerRef"

        run(queryOf(query, mapOf("bransjerRef" to bransjerRef, "orgnummer" to orgnummer.toLong())).asUpdate)
    }
}
