package no.nav.helse.tildeling

import java.util.UUID
import javax.sql.DataSource
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import org.intellij.lang.annotations.Language

class TildelingDao(private val dataSource: DataSource): HelseDao(dataSource) {

    fun opprettTildeling(oppgaveId: Long, saksbehandleroid: UUID): Boolean {
        return sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = """
                INSERT INTO tildeling (oppgave_id_ref, saksbehandler_ref)
                VALUES (:oppgave_id_ref, :saksbehandler_ref);
            """.trimIndent()
            session.transaction { tx ->
                if (tx.tildelingForOppgave(oppgaveId) != null) false
                else {
                    tx.run(
                        queryOf(
                            query, mapOf(
                                "oppgave_id_ref" to oppgaveId,
                                "saksbehandler_ref" to saksbehandleroid,
                            )
                        ).asUpdate
                    )
                    true
                }
            }
        }
    }

    fun slettTildeling(oppgaveId: Long) =
        """ DELETE
            FROM tildeling
            WHERE oppgave_id_ref = :oppgave_id_ref;
        """.update(mapOf("oppgave_id_ref" to oppgaveId))

    fun fjernPåVent(oppgaveId: Long) =
        """ UPDATE tildeling
            SET på_vent = false
            WHERE oppgave_id_ref = :oppgave_id_ref;
        """.update(mapOf("oppgave_id_ref" to oppgaveId))

    fun tildelingForPerson(fødselsnummer: String) =
        """ SELECT s.epost, s.oid, s.navn, t.på_vent FROM person
                 RIGHT JOIN vedtak v on person.id = v.person_ref
                 RIGHT JOIN oppgave o on v.id = o.vedtak_ref
                 RIGHT JOIN tildeling t on o.id = t.oppgave_id_ref
                 RIGHT JOIN saksbehandler s on t.saksbehandler_ref = s.oid
            WHERE fodselsnummer = :fnr AND o.status = 'AvventerSaksbehandler'
            ORDER BY o.opprettet DESC;
        """.single(mapOf("fnr" to fødselsnummer.toLong())) { row -> tildelingDto(row)}

    fun leggOppgavePåVent(oppgaveId: Long) =
        """ UPDATE tildeling
            SET på_vent = true
            WHERE oppgave_id_ref = :oppgave_id_ref;
        """.update(mapOf("oppgave_id_ref" to oppgaveId))

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
