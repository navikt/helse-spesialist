package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.HelseDao.Companion.asSQL
import java.time.LocalDateTime
import javax.sql.DataSource

class PgEgenAnsattDao(queryRunner: QueryRunner) : EgenAnsattDao, QueryRunner by queryRunner {
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
                (SELECT id FROM person WHERE fødselsnummer = :fodselsnummer),
                :er_egen_ansatt,
                :opprettet
            )
            ON CONFLICT (person_ref) DO UPDATE SET er_egen_ansatt = :er_egen_ansatt, opprettet = :opprettet
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
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
            WHERE p.fødselsnummer = :fodselsnummer
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
        ).singleOrNull { it.boolean("er_egen_ansatt") }
}
