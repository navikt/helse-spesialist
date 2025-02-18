package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.db.DialogDao
import no.nav.helse.db.HelseDao.Companion.asSQL
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.MedSession
import no.nav.helse.db.QueryRunner
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
