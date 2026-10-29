package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.db.DialogDao
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner

class PgDialogDao private constructor(
    queryRunner: QueryRunner,
) : DialogDao,
    QueryRunner by queryRunner {
    internal constructor(session: Session) : this(MedSession(session))

    override fun lagre(): Long =
        asSQL(
            """
            INSERT INTO dialog (opprettet)
            VALUES (now())      
            """,
        ).updateAndReturnGeneratedKey()
}
