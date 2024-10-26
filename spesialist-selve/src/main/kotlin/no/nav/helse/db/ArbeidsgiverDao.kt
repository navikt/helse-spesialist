package no.nav.helse.db

import kotliquery.Session
import kotliquery.queryOf
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.HelseDao.Companion.single
import no.nav.helse.HelseDao.Companion.update
import no.nav.helse.HelseDao.Companion.updateAndReturnGeneratedKey
import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language

class ArbeidsgiverDao(
    private val session: Session,
) {
    fun findArbeidsgiverByOrgnummer(orgnummer: String) =
        asSQL(
            "SELECT id FROM arbeidsgiver WHERE orgnummer = :orgnummer;",
            "orgnummer" to orgnummer.toLong(),
        ).single(session) { it.long("id") }

    fun insertMinimalArbeidsgiver(orgnummer: String) {
        asSQL(
            "INSERT INTO arbeidsgiver (orgnummer) VALUES (:orgnummer)",
            "orgnummer" to orgnummer.toLong(),
        ).update(session)
    }

    fun upsertNavn(
        orgnummer: String,
        navn: String,
    ) = finnArbeidsgiverNavnRef(orgnummer)
        ?.also { oppdaterArbeidsgivernavn(it, navn) }
        ?: opprettArbeidsgivernavn(orgnummer, navn)

    fun upsertBransjer(
        orgnummer: String,
        bransjer: List<String>,
    ) = session
        .finnArbeidsgiverbransjerRef(orgnummer)
        ?.also { session.oppdaterArbeidsgiverbransjer(it, bransjer) }
        ?: session.opprettArbeidsgiverbransjer(orgnummer, bransjer)

    private fun finnArbeidsgiverNavnRef(orgnummer: String) =
        asSQL(
            "SELECT navn_ref FROM arbeidsgiver WHERE orgnummer = :orgnummer",
            "orgnummer" to orgnummer.toLong(),
        ).single(session) { it.longOrNull("navn_ref") }

    private fun oppdaterArbeidsgivernavn(
        arbeidsgivernavnRef: Long,
        navn: String,
    ) {
        asSQL(
            "UPDATE arbeidsgiver_navn SET navn = :navn, navn_oppdatert = now() WHERE id = :arbeidsgivernavnRef",
            "navn" to navn,
            "arbeidsgivernavnRef" to arbeidsgivernavnRef,
        ).update(session)
    }

    private fun opprettArbeidsgivernavn(
        orgnummer: String,
        navn: String,
    ) {
        val arbeidsgivernavnId =
            asSQL(
                "INSERT INTO arbeidsgiver_navn (navn, navn_oppdatert) VALUES (:navn, now())",
                "navn" to navn,
            ).updateAndReturnGeneratedKey(session)
        checkNotNull(arbeidsgivernavnId)
        upsertNavnRef(arbeidsgivernavnId, orgnummer)
    }

    // Denne kan endres til update da det ikke ligger søknader som allerede har kommet inn på SendtSøknad-river, men ikke lest inn, igjen
    private fun upsertNavnRef(
        arbeidsgivernavnRef: Long,
        orgnummer: String,
    ) {
        asSQL(
            """
            INSERT INTO arbeidsgiver (orgnummer, navn_ref)
            VALUES (:orgnummer, :arbeidsgivernavnRef)
                ON CONFLICT (orgnummer)
                    DO UPDATE SET navn_ref = :arbeidsgivernavnRef
            """.trimIndent(),
            "arbeidsgivernavnRef" to arbeidsgivernavnRef,
            "orgnummer" to orgnummer.toLong(),
        ).update(session)
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
