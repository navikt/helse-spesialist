package no.nav.helse.spesialist.api.tildeling

import java.util.UUID
import javax.sql.DataSource
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import org.intellij.lang.annotations.Language

class TildelingDao(private val dataSource: DataSource) : HelseDao(dataSource) {

    fun opprettTildeling(oppgaveId: Long, saksbehandleroid: UUID, påVent: Boolean = false): TildelingApiDto? =
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                if (tx.tildelingForOppgave(oppgaveId) != null) null
                else {
                    tx.run {
                        asSQL(
                            """
                                WITH inserted AS (
                                    INSERT INTO tildeling (oppgave_id_ref, saksbehandler_ref, på_vent)
                                    VALUES (:oppgave_id, :saksbehandler_oid, :paa_vent)
                                    RETURNING *
                                )
                                SELECT s.navn, epost, oid, på_vent FROM inserted i
                                INNER JOIN saksbehandler s on s.oid = i.saksbehandler_ref
                            """, mapOf(
                                "saksbehandler_oid" to saksbehandleroid,
                                "oppgave_id" to oppgaveId,
                                "paa_vent" to påVent,
                            )
                        ).single(::tildelingDto)
                    }
                }
            }
        }

    fun slettTildeling(oppgaveId: Long) = asSQL(
        """ 
            DELETE
            FROM tildeling
            WHERE oppgave_id_ref = :oppgave_id_ref;
        """, mapOf("oppgave_id_ref" to oppgaveId)
    ).update()

    fun tildelingForPerson(fødselsnummer: String) = asSQL(
        """ 
            SELECT s.epost, s.oid, s.navn, t.på_vent FROM person
                 RIGHT JOIN vedtak v on person.id = v.person_ref
                 RIGHT JOIN oppgave o on v.id = o.vedtak_ref
                 RIGHT JOIN tildeling t on o.id = t.oppgave_id_ref
                 RIGHT JOIN saksbehandler s on t.saksbehandler_ref = s.oid
            WHERE fodselsnummer = :fnr AND o.status = 'AvventerSaksbehandler'
            ORDER BY o.opprettet DESC;
        """, mapOf("fnr" to fødselsnummer.toLong())
    ).single(::tildelingDto)

    private fun tildelingDto(it: Row) = TildelingApiDto(
        epost = it.string("epost"),
        påVent = it.boolean("på_vent"),
        oid = UUID.fromString(it.string("oid")),
        navn = it.string("navn")
    )

    fun tildelingForOppgave(oppgaveId: Long): TildelingApiDto? =
        sessionOf(dataSource).use { it.tildelingForOppgave(oppgaveId) }

    // ikke HelseDaoifiser denne før vi er klare for å håndtere flere queries per transaksjon
    private fun Session.tildelingForOppgave(oppgaveId: Long): TildelingApiDto? {
        @Language("PostgreSQL")
        val query = """
            SELECT s.oid, s.epost, s.navn, t.på_vent FROM tildeling t
                INNER JOIN saksbehandler s on s.oid = t.saksbehandler_ref
            WHERE t.oppgave_id_ref = :oppgaveId
            """
        return run(queryOf(query, mapOf("oppgaveId" to oppgaveId)).map(::tildelingDto).asSingle)
    }
}
