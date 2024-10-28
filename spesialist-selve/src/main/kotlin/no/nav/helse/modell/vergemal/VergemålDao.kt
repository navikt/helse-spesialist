package no.nav.helse.modell.vergemal

import kotliquery.Session
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.db.MedSession
import no.nav.helse.db.QueryRunner
import no.nav.helse.db.VergemålRepository
import java.time.LocalDateTime

data class VergemålOgFremtidsfullmakt(
    val harVergemål: Boolean,
    val harFremtidsfullmakter: Boolean,
)

class VergemålDao(session: Session) : VergemålRepository, QueryRunner by MedSession(session) {
    override fun lagre(
        fødselsnummer: String,
        vergemålOgFremtidsfullmakt: VergemålOgFremtidsfullmakt,
        fullmakt: Boolean,
    ) {
        asSQL(
            """
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
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer.toLong(),
            "har_vergemal" to vergemålOgFremtidsfullmakt.harVergemål,
            "har_fremtidsfullmakter" to vergemålOgFremtidsfullmakt.harFremtidsfullmakter,
            "har_fullmakter" to fullmakt,
            "oppdatert" to LocalDateTime.now(),
        ).update()
    }

    override fun harVergemål(fødselsnummer: String): Boolean? =
        asSQL(
            """
            SELECT har_vergemal
            FROM vergemal v
                INNER JOIN person p on p.id = v.person_ref
            WHERE p.fodselsnummer = :fodselsnummer
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer.toLong(),
        ).singleOrNull { it.boolean("har_vergemal") }

    fun harFullmakt(fødselsnummer: String): Boolean? =
        asSQL(
            """
            SELECT har_fremtidsfullmakter, har_fullmakter
                FROM vergemal v
                    INNER JOIN person p on p.id = v.person_ref
                WHERE p.fodselsnummer = :fodselsnummer
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer.toLong(),
        ).singleOrNull { it.boolean("har_fremtidsfullmakter") || it.boolean("har_fullmakter") }
}
