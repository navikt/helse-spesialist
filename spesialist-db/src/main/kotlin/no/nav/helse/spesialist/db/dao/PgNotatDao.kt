package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.db.NotatDao
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.NotatType
import java.util.UUID
import javax.sql.DataSource

class PgNotatDao private constructor(
    queryRunner: QueryRunner,
) : NotatDao,
    QueryRunner by queryRunner {
    internal constructor(session: Session) : this(MedSession(session))
    internal constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    override fun lagreForOppgaveId(
        oppgaveId: Long,
        tekst: String,
        saksbehandlerOid: UUID,
        notatType: NotatType,
        dialogRef: Long,
    ): Long? =
        asSQL(
            """
            INSERT INTO notat (vedtaksperiode_id, tekst, saksbehandler_oid, type, dialog_ref)
            VALUES (
                (SELECT v.vedtaksperiode_id 
                    FROM vedtak v 
                    INNER JOIN oppgave o on v.id = o.vedtak_ref 
                    WHERE o.id = :oppgave_id), 
                :tekst, 
                :saksbehandler_oid,
                CAST(:type as notattype),
                :dialog_ref
            )      
            """,
            "oppgave_id" to oppgaveId,
            "tekst" to tekst,
            "saksbehandler_oid" to saksbehandlerOid,
            "type" to notatType.name,
            "dialog_ref" to dialogRef,
        ).updateAndReturnGeneratedKeyOrNull()
}
