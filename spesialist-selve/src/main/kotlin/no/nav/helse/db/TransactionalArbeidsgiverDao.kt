package no.nav.helse.db

import kotliquery.Session
import kotliquery.queryOf
import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language

class TransactionalArbeidsgiverDao(
    private val session: Session,
) {
    fun findArbeidsgiverByOrgnummer(orgnummer: String) =
        session.run(
            queryOf("SELECT id FROM arbeidsgiver WHERE orgnummer=?;", orgnummer.toLong())
                .map { it.long("id") }
                .asSingle,
        )

    fun insertMinimalArbeidsgiver(orgnummer: String) {
        @Language("PostgreSQL")
        val query = "INSERT INTO arbeidsgiver(orgnummer) VALUES(:orgnummer)"

        session.run(queryOf(query, mapOf("orgnummer" to orgnummer.toLong())).asUpdate)
    }

    fun upsertNavn(
        orgnummer: String,
        navn: String,
    ) = session
        .finnArbeidsgiverNavnRef(orgnummer)
        ?.also { session.oppdaterArbeidsgivernavn(it, navn) }
        ?: session.opprettArbeidsgivernavn(orgnummer, navn)

    fun upsertBransjer(
        orgnummer: String,
        bransjer: List<String>,
    ) = session
        .finnArbeidsgiverbransjerRef(orgnummer)
        ?.also { session.oppdaterArbeidsgiverbransjer(it, bransjer) }
        ?: session.opprettArbeidsgiverbransjer(orgnummer, bransjer)

    private fun Session.finnArbeidsgiverNavnRef(orgnummer: String): Long? {
        @Language("PostgreSQL")
        val query = "SELECT navn_ref FROM arbeidsgiver WHERE orgnummer=?"

        return run(queryOf(query, orgnummer.toLong()).map { it.longOrNull("navn_ref") }.asSingle)
    }

    private fun Session.oppdaterArbeidsgivernavn(
        arbeidsgivernavnRef: Long,
        navn: String,
    ) {
        @Language("PostgreSQL")
        val query = "UPDATE arbeidsgiver_navn SET navn=:navn, navn_oppdatert=now() WHERE id=:arbeidsgivernavnRef"

        run(queryOf(query, mapOf("navn" to navn, "arbeidsgivernavnRef" to arbeidsgivernavnRef)).asUpdate)
    }

    private fun Session.opprettArbeidsgivernavn(
        orgnummer: String,
        navn: String,
    ) {
        @Language("PostgreSQL")
        val query = "INSERT INTO arbeidsgiver_navn (navn, navn_oppdatert) VALUES (?, now())"

        val arbeidsgivernavnId = requireNotNull(run(queryOf(query, navn).asUpdateAndReturnGeneratedKey))
        upsertNavnRef(arbeidsgivernavnId, orgnummer)
    }

    // Denne kan endres til update da det ikke ligger søknader som allerede har kommet inn på SendtSøknad-river, men ikke lest inn, igjen
    private fun Session.upsertNavnRef(
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

    private fun Session.finnArbeidsgiverbransjerRef(orgnummer: String): Long? {
        @Language("PostgreSQL")
        val query = "SELECT bransjer_ref FROM arbeidsgiver WHERE orgnummer=?"

        return run(queryOf(query, orgnummer.toLong()).map { it.longOrNull("bransjer_ref") }.asSingle)
    }

    private fun Session.oppdaterArbeidsgiverbransjer(
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

    private fun Session.opprettArbeidsgiverbransjer(
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
    private fun Session.upsertBransjerRef(
        bransjerRef: Long,
        orgnummer: String,
    ) {
        @Language("PostgreSQL")
        val query =
            "INSERT INTO arbeidsgiver (orgnummer, bransjer_ref) VALUES(:orgnummer, :bransjerRef) ON CONFLICT(orgnummer) DO UPDATE SET bransjer_ref=:bransjerRef"

        run(queryOf(query, mapOf("bransjerRef" to bransjerRef, "orgnummer" to orgnummer.toLong())).asUpdate)
    }
}
