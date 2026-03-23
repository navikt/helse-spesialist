package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.db.VergemålDao
import no.nav.helse.db.VergemålOgFremtidsfullmakt
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import java.time.LocalDateTime

class PgVergemålDao internal constructor(
    session: Session,
) : VergemålDao,
    QueryRunner by MedSession(session) {
    override fun lagre(
        fødselsnummer: String,
        vergemålOgFremtidsfullmakt: VergemålOgFremtidsfullmakt,
        fullmakt: Boolean,
    ) {
        asSQL(
            """
            INSERT INTO vergemal (person_ref, har_vergemal, har_fremtidsfullmakter, har_fullmakter, vergemål_oppdatert, fullmakt_oppdatert)
            VALUES (
                (SELECT id FROM person WHERE fødselsnummer = :fodselsnummer),
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
            "fodselsnummer" to fødselsnummer,
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
            WHERE p.fødselsnummer = :fodselsnummer
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
        ).singleOrNull { it.boolean("har_vergemal") }

    override fun harFullmakt(fødselsnummer: String): Boolean? =
        asSQL(
            """
            SELECT har_fremtidsfullmakter, har_fullmakter
                FROM vergemal v
                    INNER JOIN person p on p.id = v.person_ref
                WHERE p.fødselsnummer = :fodselsnummer
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
        ).singleOrNull { it.boolean("har_fremtidsfullmakter") || it.boolean("har_fullmakter") }
}
