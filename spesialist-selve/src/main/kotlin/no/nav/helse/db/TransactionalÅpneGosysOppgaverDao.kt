package no.nav.helse.db

import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.helse.modell.gosysoppgaver.ÅpneGosysOppgaverDto
import org.intellij.lang.annotations.Language

class TransactionalÅpneGosysOppgaverDao(private val transactionalSession: TransactionalSession) : ÅpneGosysOppgaverRepository {
    override fun persisterÅpneGosysOppgaver(åpneGosysOppgaver: ÅpneGosysOppgaverDto) {
        @Language("PostgreSQL")
        val query =
            """
            INSERT INTO gosysoppgaver (person_ref, antall, oppslag_feilet, opprettet)
            VALUES ((SELECT id FROM person WHERE fodselsnummer = :fodselsnummer), :antall, :oppslag_feilet, :opprettet)
            ON CONFLICT (person_ref) DO UPDATE SET antall = :antall, oppslag_feilet = :oppslag_feilet, opprettet = :opprettet
            """.trimIndent()
        transactionalSession.run(
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

    override fun antallÅpneOppgaver(fødselsnummer: String): Int? {
        @Language("PostgreSQL")
        val query =
            """
            SELECT go.antall
            FROM gosysoppgaver go
                INNER JOIN person p on p.id = go.person_ref
            WHERE p.fodselsnummer = ?
            AND go.oppslag_feilet = FALSE
            """.trimIndent()
        return transactionalSession.run(
            queryOf(query, fødselsnummer.toLong())
                .map { it.intOrNull("antall") }
                .asSingle,
        )
    }
}
