package no.nav.helse.modell.gosysoppgaver

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

internal class ÅpneGosysOppgaverDao(val dataSource: DataSource) {
    internal fun persisterÅpneGosysOppgaver(åpneGosysOppgaver: ÅpneGosysOppgaverDto) {
        @Language("PostgreSQL")
        val query = """
                INSERT INTO gosysoppgaver (person_ref, antall, oppslag_feilet, opprettet)
                VALUES ((SELECT id FROM person WHERE fodselsnummer = :fodselsnummer), :antall, :oppslag_feilet, :opprettet)
                ON CONFLICT (person_ref) DO UPDATE SET antall = :antall, oppslag_feilet = :oppslag_feilet, opprettet = :opprettet
                """
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "fodselsnummer" to åpneGosysOppgaver.fødselsnummer.toLong(),
                        "antall" to åpneGosysOppgaver.antall,
                        "oppslag_feilet" to åpneGosysOppgaver.oppslagFeilet,
                        "opprettet" to åpneGosysOppgaver.opprettet,
                    ),
                ).asExecute,
            )
        }
    }

    internal fun harÅpneOppgaver(fødselsnummer: String): Int? {
        @Language("PostgreSQL")
        val query = """
                SELECT go.antall
                FROM gosysoppgaver go
                         INNER JOIN person p on p.id = go.person_ref
                WHERE p.fodselsnummer = ?
                AND go.oppslag_feilet = FALSE
                """
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(query, fødselsnummer.toLong())
                    .map { it.intOrNull("antall") }
                    .asSingle,
            )
        }
    }
}
