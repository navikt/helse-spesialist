package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.HelseDao.Companion.asSQL
import javax.sql.DataSource

class PgDialogDao(
    queryRunner: QueryRunner,
) : DialogDao,
    QueryRunner by queryRunner {
    constructor(session: Session) : this(MedSession(session))
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    override fun lagre(): Long =
        asSQL(
            """
            INSERT INTO dialog (opprettet)
            VALUES (now())      
            """,
        ).updateAndReturnGeneratedKey()
}
