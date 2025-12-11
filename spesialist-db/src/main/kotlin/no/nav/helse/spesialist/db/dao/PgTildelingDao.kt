package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.db.TildelingDao
import no.nav.helse.db.TildelingDto
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedDataSource
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import javax.sql.DataSource

class PgTildelingDao internal constructor(
    queryRunner: QueryRunner,
) : TildelingDao,
    QueryRunner by queryRunner {
    internal constructor(session: Session) : this(MedSession(session))
    internal constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    fun tildel(
        oppgaveId: Long,
        saksbehandlerOid: SaksbehandlerOid,
    ) {
        asSQL(
            """
            INSERT INTO tildeling (saksbehandler_ref, oppgave_id_ref) VALUES (:oid, :oppgave_id)
            ON CONFLICT (oppgave_id_ref) DO UPDATE SET saksbehandler_ref = :oid
            """.trimIndent(),
            "oid" to saksbehandlerOid.value,
            "oppgave_id" to oppgaveId,
        ).update()
    }

    fun avmeld(oppgaveId: Long) {
        asSQL(
            """
            DELETE FROM tildeling WHERE oppgave_id_ref = :oppgave_id
            """.trimIndent(),
            "oppgave_id" to oppgaveId,
        ).update()
    }

    override fun tildelingForPerson(fødselsnummer: String) =
        asSQL(
            """
            SELECT s.epost, s.oid, s.navn FROM person p
            JOIN vedtaksperiode v on p.id = v.person_ref
            JOIN oppgave o on v.id = o.vedtak_ref
            JOIN tildeling t on o.id = t.oppgave_id_ref
            JOIN saksbehandler s on t.saksbehandler_ref = s.oid
            WHERE fødselsnummer = :foedselsnummer AND o.status = 'AvventerSaksbehandler'
            ORDER BY o.opprettet DESC;
            """.trimIndent(),
            "foedselsnummer" to fødselsnummer,
        ).singleOrNull {
            TildelingDto(
                navn = it.string("navn"),
                epost = it.string("epost"),
                oid = it.uuid("oid"),
            )
        }

    override fun tildelingForOppgave(oppgaveId: Long) =
        asSQL(
            """
            SELECT s.oid, s.epost, s.navn FROM tildeling t
            INNER JOIN saksbehandler s on s.oid = t.saksbehandler_ref
            WHERE t.oppgave_id_ref = :oppgaveId
            """.trimIndent(),
            "oppgaveId" to oppgaveId,
        ).singleOrNull {
            TildelingDto(
                navn = it.string("navn"),
                epost = it.string("epost"),
                oid = it.uuid("oid"),
            )
        }
}
