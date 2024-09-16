package no.nav.helse.modell.arbeidsgiver

import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

class ArbeidsgiverDao(private val dataSource: DataSource) {
    fun findArbeidsgiverByOrgnummer(orgnummer: String) =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf("SELECT id FROM arbeidsgiver WHERE orgnummer=?;", orgnummer.toLong())
                    .map { it.long("id") }
                    .asSingle,
            )
        }

    fun TransactionalSession.insertMinimalArbeidsgiver(orgnummer: String) {
        @Language("PostgreSQL")
        val query = "INSERT INTO arbeidsgiver(orgnummer) VALUES(:orgnummer)"

        run(queryOf(query, mapOf("orgnummer" to orgnummer.toLong())).asUpdate)
    }

    fun TransactionalSession.upsertNavn(
        orgnummer: String,
        navn: String,
    ) = this.finnArbeidsgiverNavnRef(orgnummer)
        ?.also { this.oppdaterArbeidsgivernavn(it, navn) }
        ?: this.opprettArbeidsgivernavn(orgnummer, navn)

    fun TransactionalSession.upsertBransjer(
        orgnummer: String,
        bransjer: List<String>,
    ) = this.finnArbeidsgiverbransjerRef(orgnummer)
        ?.also { this.oppdaterArbeidsgiverbransjer(it, bransjer) }
        ?: this.opprettArbeidsgiverbransjer(orgnummer, bransjer)

    private fun TransactionalSession.finnArbeidsgiverNavnRef(orgnummer: String): Long? {
        @Language("PostgreSQL")
        val query = "SELECT navn_ref FROM arbeidsgiver WHERE orgnummer=?"

        return run(queryOf(query, orgnummer.toLong()).map { it.longOrNull("navn_ref") }.asSingle)
    }

    private fun TransactionalSession.oppdaterArbeidsgivernavn(
        arbeidsgivernavnRef: Long,
        navn: String,
    ) {
        @Language("PostgreSQL")
        val query = "UPDATE arbeidsgiver_navn SET navn=:navn, navn_oppdatert=now() WHERE id=:arbeidsgivernavnRef"

        run(queryOf(query, mapOf("navn" to navn, "arbeidsgivernavnRef" to arbeidsgivernavnRef)).asUpdate)
    }

    private fun TransactionalSession.opprettArbeidsgivernavn(
        orgnummer: String,
        navn: String,
    ) {
        @Language("PostgreSQL")
        val query = "INSERT INTO arbeidsgiver_navn (navn, navn_oppdatert) VALUES (?, now())"

        val arbeidsgivernavnId = requireNotNull(run(queryOf(query, navn).asUpdateAndReturnGeneratedKey))
        upsertNavnRef(arbeidsgivernavnId, orgnummer)
    }

    // Denne kan endres til update da det ikke ligger søknader som allerede har kommet inn på SendtSøknad-river, men ikke lest inn, igjen
    private fun TransactionalSession.upsertNavnRef(
        arbeidsgivernavnRef: Long,
        orgnummer: String,
    ) {
        @Language("PostgreSQL")
        val query = "INSERT INTO arbeidsgiver (orgnummer, navn_ref) VALUES(:orgnummer, :arbeidsgivernavnRef) ON CONFLICT(orgnummer) DO UPDATE SET navn_ref=:arbeidsgivernavnRef"

        run(queryOf(query, mapOf("arbeidsgivernavnRef" to arbeidsgivernavnRef, "orgnummer" to orgnummer.toLong())).asUpdate)
    }

    private fun TransactionalSession.finnArbeidsgiverbransjerRef(orgnummer: String): Long? {
        @Language("PostgreSQL")
        val query = "SELECT bransjer_ref FROM arbeidsgiver WHERE orgnummer=?"

        return run(queryOf(query, orgnummer.toLong()).map { it.longOrNull("bransjer_ref") }.asSingle)
    }

    private fun TransactionalSession.oppdaterArbeidsgiverbransjer(
        bransjerRef: Long,
        bransjer: List<String>,
    ) {
        @Language("PostgreSQL")
        val query = "UPDATE arbeidsgiver_bransjer SET bransjer=:bransjer, oppdatert=now() WHERE id=:bransjerRef"

        run(queryOf(query, mapOf("bransjer" to objectMapper.writeValueAsString(bransjer), "bransjerRef" to bransjerRef)).asUpdate)
    }

    private fun TransactionalSession.opprettArbeidsgiverbransjer(
        orgnummer: String,
        bransjer: List<String>,
    ) {
        @Language("PostgreSQL")
        val query = "INSERT INTO arbeidsgiver_bransjer (bransjer, oppdatert) VALUES (?, now())"

        val bransjerId = requireNotNull(run(queryOf(query, objectMapper.writeValueAsString(bransjer)).asUpdateAndReturnGeneratedKey))
        upsertBransjerRef(bransjerId, orgnummer)
    }

    // Denne kan endres til update da det ikke ligger søknader som allerede har kommet inn på SendtSøknad-river, men ikke lest inn, igjen
    private fun TransactionalSession.upsertBransjerRef(
        bransjerRef: Long,
        orgnummer: String,
    ) {
        @Language("PostgreSQL")
        val query = "INSERT INTO arbeidsgiver (orgnummer, bransjer_ref) VALUES(:orgnummer, :bransjerRef) ON CONFLICT(orgnummer) DO UPDATE SET bransjer_ref=:bransjerRef"

        run(queryOf(query, mapOf("bransjerRef" to bransjerRef, "orgnummer" to orgnummer.toLong())).asUpdate)
    }
}
