package no.nav.helse.db

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.util.UUID
import javax.sql.DataSource

class TildelingDao(private val dataSource: DataSource) : TildelingRepository {
    fun tildel(
        oppgaveId: Long,
        saksbehandlerOid: UUID,
    ) {
        @Language("PostgreSQL")
        val query = """
            INSERT INTO tildeling(saksbehandler_ref, oppgave_id_ref) VALUES (:oid, :oppgave_id)
            ON CONFLICT (oppgave_id_ref) DO UPDATE SET saksbehandler_ref = :oid
        """

        sessionOf(dataSource).use {
            it.run(
                queryOf(
                    query,
                    mapOf(
                        "oid" to saksbehandlerOid,
                        "oppgave_id" to oppgaveId,
                    ),
                ).asUpdate,
            )
        }
    }

    fun avmeld(oppgaveId: Long) {
        @Language("PostgreSQL")
        val query = """
            DELETE FROM tildeling WHERE oppgave_id_ref = :oppgave_id
        """

        sessionOf(dataSource).use {
            it.run(queryOf(query, mapOf("oppgave_id" to oppgaveId)).asUpdate)
        }
    }

    override fun tildelingForPerson(fødselsnummer: String): TildelingDto? {
        @Language("PostgreSQL")
        val query = """ 
            SELECT s.epost, s.oid, s.navn FROM person
                 RIGHT JOIN vedtak v on person.id = v.person_ref
                 RIGHT JOIN oppgave o on v.id = o.vedtak_ref
                 RIGHT JOIN tildeling t on o.id = t.oppgave_id_ref
                 RIGHT JOIN saksbehandler s on t.saksbehandler_ref = s.oid
            WHERE fodselsnummer = :fnr AND o.status = 'AvventerSaksbehandler'
            ORDER BY o.opprettet DESC;
        """
        return sessionOf(dataSource).use {
            it.run(
                queryOf(query, mapOf("fnr" to fødselsnummer.toLong())).map { row ->
                    TildelingDto(
                        navn = row.string("navn"),
                        epost = row.string("epost"),
                        oid = UUID.fromString(row.string("oid")),
                    )
                }.asSingle,
            )
        }
    }

    override fun tildelingForOppgave(oppgaveId: Long): TildelingDto? {
        @Language("PostgreSQL")
        val query = """ 
            SELECT s.oid, s.epost, s.navn FROM tildeling t
                INNER JOIN saksbehandler s on s.oid = t.saksbehandler_ref
            WHERE t.oppgave_id_ref = :oppgaveId
        """
        return sessionOf(dataSource).use {
            it.run(
                queryOf(query, mapOf("oppgaveId" to oppgaveId)).map { row ->
                    TildelingDto(
                        navn = row.string("navn"),
                        epost = row.string("epost"),
                        oid = UUID.fromString(row.string("oid")),
                    )
                }.asSingle,
            )
        }
    }
}
