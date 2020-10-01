package no.nav.helse.modell.dkif

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

internal class DigitalKontaktinformasjonDao(val dataSource: DataSource) {
    internal fun persisterDigitalKontaktinformasjon(digitalKontaktinformasjon: DigitalKontaktinformasjonDto) {
        @Language("PostgreSQL")
        val query = "INSERT INTO digital_kontaktinformasjon (fodselsnummer, er_digital, opprettet) VALUES (?, ?, ?)"
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    digitalKontaktinformasjon.fødselsnummer.toLong(),
                    digitalKontaktinformasjon.erDigital,
                    digitalKontaktinformasjon.opprettet
                ).asExecute
            )
        }
    }

    internal fun erDigital(fødselsnummer: String): Boolean? {
        @Language("PostgreSQL")
        val query =
            "SELECT er_digital FROM digital_kontaktinformasjon WHERE fodselsnummer = ? ORDER BY opprettet DESC LIMIT 1"
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(query, fødselsnummer.toLong())
                    .map { it.boolean("er_digital") }
                    .asSingle
            )
        }
    }
}
