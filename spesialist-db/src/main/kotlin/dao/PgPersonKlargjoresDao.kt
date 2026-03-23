package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.spesialist.application.PersonKlargjoresDao
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import java.time.LocalDateTime

internal class PgPersonKlargjoresDao(
    session: Session,
) : QueryRunner by MedSession(session),
    PersonKlargjoresDao {
    override fun personKlargjøres(fødselsnummer: String) {
        asSQL(
            "INSERT INTO person_klargjores(fødselsnummer, opprettet) VALUES(:fodselsnummer, :opprettet) ON CONFLICT DO NOTHING",
            "fodselsnummer" to fødselsnummer,
            "opprettet" to LocalDateTime.now(),
        ).update()
    }

    override fun klargjøringPågår(fødselsnummer: String): Boolean =
        asSQL(
            "SELECT true FROM person_klargjores WHERE fødselsnummer = :fodselsnummer",
            "fodselsnummer" to fødselsnummer,
            "opprettet" to LocalDateTime.now(),
        ).singleOrNull { it.boolean(1) } ?: false

    override fun personKlargjort(fødselsnummer: String) {
        asSQL(
            "DELETE FROM person_klargjores WHERE fødselsnummer = :foedselsnummer",
            "foedselsnummer" to fødselsnummer,
        ).update()
    }
}
