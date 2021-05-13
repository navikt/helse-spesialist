package no.nav.helse.modell.dkif

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import javax.sql.DataSource

internal class DigitalKontaktinformasjonDao(val dataSource: DataSource) {
    internal fun lagre(fødselsnummer: String, erDigital: Boolean, opprettet: LocalDateTime) {
        @Language("PostgreSQL")
        val query = """
            INSERT INTO digital_kontaktinformasjon (person_ref, er_digital, opprettet)
            VALUES (
                (SELECT id FROM person WHERE fodselsnummer = :fodselsnummer),
                :er_digital,
                :opprettet
            )
            ON CONFLICT (person_ref) DO UPDATE SET er_digital = :er_digital, opprettet = :opprettet
        """
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "fodselsnummer" to fødselsnummer.toLong(),
                        "er_digital" to erDigital,
                        "opprettet" to opprettet
                    )
                ).asExecute
            )
        }
    }

    internal fun erDigital(fødselsnummer: String): Boolean? {
        @Language("PostgreSQL")
        val query = """
            SELECT er_digital
            FROM digital_kontaktinformasjon dkif
                INNER JOIN person p on p.id = dkif.person_ref
            WHERE p.fodselsnummer = ?
        """
        return sessionOf(dataSource).use { session ->
            session.run(queryOf(query, fødselsnummer.toLong()).map { it.boolean("er_digital") }.asSingle)
        }
    }
}
