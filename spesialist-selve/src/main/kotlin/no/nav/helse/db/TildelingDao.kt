package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.HelseDao.Companion.asSQL
import java.util.UUID
import javax.sql.DataSource

class TildelingDao(queryRunner: QueryRunner) : TildelingRepository, QueryRunner by queryRunner {
    constructor(session: Session) : this(MedSession(session))
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    override fun tildel(
        oppgaveId: Long,
        saksbehandlerOid: UUID,
    ) {
        asSQL(
            """
            INSERT INTO tildeling (saksbehandler_ref, oppgave_id_ref) VALUES (:oid, :oppgave_id)
            ON CONFLICT (oppgave_id_ref) DO UPDATE SET saksbehandler_ref = :oid
            """.trimIndent(),
            "oid" to saksbehandlerOid,
            "oppgave_id" to oppgaveId,
        ).update()
    }

    override fun avmeld(oppgaveId: Long) {
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
            SELECT s.epost, s.oid, s.navn FROM person
                 RIGHT JOIN vedtak v on person.id = v.person_ref
                 RIGHT JOIN oppgave o on v.id = o.vedtak_ref
                 RIGHT JOIN tildeling t on o.id = t.oppgave_id_ref
                 RIGHT JOIN saksbehandler s on t.saksbehandler_ref = s.oid
            WHERE fodselsnummer = :foedselsnummer AND o.status = 'AvventerSaksbehandler'
            ORDER BY o.opprettet DESC;
            """.trimIndent(),
            "foedselsnummer" to fødselsnummer.toLong(),
        ).single {
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
        ).single {
            TildelingDto(
                navn = it.string("navn"),
                epost = it.string("epost"),
                oid = it.uuid("oid"),
            )
        }
}
