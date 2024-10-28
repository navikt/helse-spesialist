package no.nav.helse.modell.gosysoppgaver

import kotliquery.Session
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.db.MedSession
import no.nav.helse.db.QueryRunner
import no.nav.helse.db.ÅpneGosysOppgaverRepository

internal class ÅpneGosysOppgaverDao(session: Session) : ÅpneGosysOppgaverRepository, QueryRunner by MedSession(session) {
    override fun persisterÅpneGosysOppgaver(åpneGosysOppgaver: ÅpneGosysOppgaverDto) {
        asSQL(
            """
            INSERT INTO gosysoppgaver (person_ref, antall, oppslag_feilet, opprettet)
            VALUES ((SELECT id FROM person WHERE fodselsnummer = :fodselsnummer), :antall, :oppslag_feilet, :opprettet)
            ON CONFLICT (person_ref) DO UPDATE SET antall = :antall, oppslag_feilet = :oppslag_feilet, opprettet = :opprettet
            """.trimIndent(),
            "fodselsnummer" to åpneGosysOppgaver.fødselsnummer.toLong(),
            "antall" to åpneGosysOppgaver.antall,
            "oppslag_feilet" to åpneGosysOppgaver.oppslagFeilet,
            "opprettet" to åpneGosysOppgaver.opprettet,
        ).update()
    }

    override fun antallÅpneOppgaver(fødselsnummer: String): Int? =
        asSQL(
            """
            SELECT go.antall
            FROM gosysoppgaver go
                INNER JOIN person p on p.id = go.person_ref
            WHERE p.fodselsnummer = :fodselsnummer
            AND go.oppslag_feilet = FALSE
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer.toLong(),
        ).singleOrNull { it.int("antall") }
}
