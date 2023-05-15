package no.nav.helse.spesialist.api.tildeling

import java.util.UUID
import javax.sql.DataSource
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.HelseDao
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TildelingDao(private val dataSource: DataSource) : HelseDao(dataSource) {

    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    fun opprettTildeling(oppgaveId: Long, saksbehandleroid: UUID, påVent: Boolean = false): TildelingApiDto? =
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                if (tx.tildelingForOppgave(oppgaveId) != null) null
                else {
                    val key = tx.run {
                        queryize(
                            """
                                INSERT INTO tildeling (oppgave_id_ref, saksbehandler_ref, på_vent)
                                VALUES (:oppgave_id_ref, :saksbehandler_ref, :paa_vent);
                            """
                        ).update(
                            mapOf(
                                "oppgave_id_ref" to oppgaveId,
                                "saksbehandler_ref" to saksbehandleroid,
                                "paa_vent" to påVent,
                            )
                        )
                    }
                    if (key > 0) {
                        tx.run {
                            queryize(
                                """
                                SELECT navn, epost, oid, på_vent
                                FROM saksbehandler s
                                INNER JOIN tildeling t on s.oid = t.saksbehandler_ref AND t.oppgave_id_ref = :oppgave_id
                                WHERE s.oid = :saksbehandler_oid
                            """
                            ).single(
                                mapOf("saksbehandler_oid" to saksbehandleroid, "oppgave_id" to oppgaveId),
                                ::tildelingDto
                            )
                        }
                    } else null
                }
            }
        }.also {
            sikkerlogg.info("fra tildelingdao: {}", kv("TildeingApiDto", it))
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
        """.single(mapOf("fnr" to fødselsnummer.toLong())) { row -> tildelingDto(row) }

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
