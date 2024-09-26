package no.nav.helse.db

import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language

class TransactionalArbeidsgiverDao(
    private val transactionalSession: TransactionalSession,
) {
    fun findArbeidsgiverByOrgnummer(orgnummer: String) =
        transactionalSession.run(
            queryOf("SELECT id FROM arbeidsgiver WHERE orgnummer=?;", orgnummer.toLong())
                .map { it.long("id") }
                .asSingle,
        )

    fun insertMinimalArbeidsgiver(orgnummer: String) {
        @Language("PostgreSQL")
        val query = "INSERT INTO arbeidsgiver(orgnummer) VALUES(:orgnummer)"

        transactionalSession.run(queryOf(query, mapOf("orgnummer" to orgnummer.toLong())).asUpdate)
    }

    fun upsertNavn(
        orgnummer: String,
        navn: String,
    ) = transactionalSession
        .finnArbeidsgiverNavnRef(orgnummer)
        ?.also { transactionalSession.oppdaterArbeidsgivernavn(it, navn) }
        ?: transactionalSession.opprettArbeidsgivernavn(orgnummer, navn)

    fun upsertBransjer(
        orgnummer: String,
        bransjer: List<String>,
    ) = transactionalSession
        .finnArbeidsgiverbransjerRef(orgnummer)
        ?.also { transactionalSession.oppdaterArbeidsgiverbransjer(it, bransjer) }
        ?: transactionalSession.opprettArbeidsgiverbransjer(orgnummer, bransjer)

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
        val query =
            "INSERT INTO arbeidsgiver (orgnummer, navn_ref) VALUES(:orgnummer, :arbeidsgivernavnRef) ON CONFLICT(orgnummer) DO UPDATE SET navn_ref=:arbeidsgivernavnRef"

        run(
            queryOf(
                query,
                mapOf("arbeidsgivernavnRef" to arbeidsgivernavnRef, "orgnummer" to orgnummer.toLong()),
            ).asUpdate,
        )
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

        run(
            queryOf(
                query,
                mapOf("bransjer" to objectMapper.writeValueAsString(bransjer), "bransjerRef" to bransjerRef),
            ).asUpdate,
        )
    }

    private fun TransactionalSession.opprettArbeidsgiverbransjer(
        orgnummer: String,
        bransjer: List<String>,
    ) {
        @Language("PostgreSQL")
        val query = "INSERT INTO arbeidsgiver_bransjer (bransjer, oppdatert) VALUES (?, now())"

        val bransjerId =
            requireNotNull(run(queryOf(query, objectMapper.writeValueAsString(bransjer)).asUpdateAndReturnGeneratedKey))
        upsertBransjerRef(bransjerId, orgnummer)
    }

    // Denne kan endres til update da det ikke ligger søknader som allerede har kommet inn på SendtSøknad-river, men ikke lest inn, igjen
    private fun TransactionalSession.upsertBransjerRef(
        bransjerRef: Long,
        orgnummer: String,
    ) {
        @Language("PostgreSQL")
        val query =
            "INSERT INTO arbeidsgiver (orgnummer, bransjer_ref) VALUES(:orgnummer, :bransjerRef) ON CONFLICT(orgnummer) DO UPDATE SET bransjer_ref=:bransjerRef"

        run(queryOf(query, mapOf("bransjerRef" to bransjerRef, "orgnummer" to orgnummer.toLong())).asUpdate)
    }
}
