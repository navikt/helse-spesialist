package no.nav.helse.modell.egenansatt

import kotliquery.Session
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.db.EgenAnsattRepository
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.MedSession
import no.nav.helse.db.QueryRunner
import java.time.LocalDateTime
import javax.sql.DataSource

class EgenAnsattDao(queryRunner: QueryRunner) : EgenAnsattRepository, QueryRunner by queryRunner {
    constructor(session: Session) : this(MedSession(session))
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    override fun lagre(
        fødselsnummer: String,
        erEgenAnsatt: Boolean,
        opprettet: LocalDateTime,
    ) {
        asSQL(
            """
            INSERT INTO egen_ansatt (person_ref, er_egen_ansatt, opprettet)
            VALUES (
                (SELECT id FROM person WHERE fodselsnummer = :fodselsnummer),
                :er_egen_ansatt,
                :opprettet
            )
            ON CONFLICT (person_ref) DO UPDATE SET er_egen_ansatt = :er_egen_ansatt, opprettet = :opprettet
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer.toLong(),
            "er_egen_ansatt" to erEgenAnsatt,
            "opprettet" to opprettet,
        ).update()
    }

    override fun erEgenAnsatt(fødselsnummer: String): Boolean? =
        asSQL(
            """
            SELECT er_egen_ansatt
            FROM egen_ansatt ea
                INNER JOIN person p on p.id = ea.person_ref
            WHERE p.fodselsnummer = :fodselsnummer
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer.toLong(),
        ).singleOrNull { it.boolean("er_egen_ansatt") }
}
