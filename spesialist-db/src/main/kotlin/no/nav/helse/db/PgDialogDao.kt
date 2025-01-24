package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.db.HelseDao.Companion.asSQL
import javax.sql.DataSource

class PgDialogDao private constructor(
    queryRunner: QueryRunner,
) : DialogDao,
    QueryRunner by queryRunner {
    internal constructor(session: Session) : this(MedSession(session))
    internal constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    override fun lagre(): Long =
        asSQL(
            """
            INSERT INTO dialog (opprettet)
            VALUES (now())      
            """,
        ).updateAndReturnGeneratedKey()
}
