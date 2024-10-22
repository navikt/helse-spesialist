package no.nav.helse.db

import kotliquery.Session
import kotliquery.queryOf
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.HelseDao.Companion.update
import no.nav.helse.modell.vergemal.VergemålOgFremtidsfullmakt
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime

class TransactionalVergemålDao(private val session: Session) : VergemålRepository {
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
        ).update(session)
    }

    override fun harVergemål(fødselsnummer: String): Boolean? {
        @Language("PostgreSQL")
        val query =
            """
            SELECT har_vergemal
            FROM vergemal v
                INNER JOIN person p on p.id = v.person_ref
            WHERE p.fodselsnummer = :fodselsnummer
            """.trimIndent()
        return session.run(
            queryOf(
                query,
                mapOf("fodselsnummer" to fødselsnummer.toLong()),
            )
                .map { it.boolean("har_vergemal") }
                .asSingle,
        )
    }
}
