package no.nav.helse.tildeling

import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

internal class TildelingDao(private val dataSource: DataSource) {
    internal fun tildelOppgave(oppgavereferanse: UUID, saksbehandleroid: UUID) {
        using(sessionOf(dataSource)) {
            it.tildelOppgave(oppgavereferanse, saksbehandleroid)
        }
    }
}

fun Session.tildelOppgave(oppgaveReferanse: UUID, saksbehandlerOid: UUID) {
    @Language("PostgreSQL")
    val query = "INSERT INTO tildeling(oppgave_ref, saksbehandler_ref) VALUES(:oppgave_ref, :saksbehandler_ref);"
    run(
        queryOf(
            query, mapOf(
                "oppgave_ref" to oppgaveReferanse,
                "saksbehandler_ref" to saksbehandlerOid
            )
        ).asUpdate
    )
}

fun Session.hentSaksbehandlerEpostFor(oppgaveReferanse: UUID): String? {
    @Language("PostgreSQL")
    val query =
        "SELECT * FROM tildeling INNER JOIN saksbehandler s on s.oid = tildeling.saksbehandler_ref WHERE oppgave_ref=:oppgave_ref;"
    return run(queryOf(query, mapOf("oppgave_ref" to oppgaveReferanse)).map { row ->
        row.string("epost")
    }.asSingle)
}

fun Session.hentSaksbehandlerNavnFor(oppgaveReferanse: UUID): String? {
    @Language("PostgreSQL")
    val query =
        "SELECT * FROM tildeling INNER JOIN saksbehandler s on s.oid = tildeling.saksbehandler_ref WHERE oppgave_ref=:oppgave_ref;"
    return run(queryOf(query, mapOf("oppgave_ref" to oppgaveReferanse)).map { row ->
        row.string("navn")
    }.asSingle)
}

fun Session.slettOppgavetildeling(oppgaveReferanse: UUID) {
    @Language("PostgreSQL")
    val query = "DELETE FROM tildeling WHERE oppgave_ref=:oppgave_ref;"
    run(queryOf(query, mapOf("oppgave_ref" to oppgaveReferanse)).asUpdate)
}

fun Session.tildelingForPerson(fødselsnummer: String): String? {
    @Language("PostgreSQL")
    val query = """
SELECT epost FROM person
     RIGHT JOIN vedtak v on person.id = v.person_ref
     RIGHT JOIN oppgave o on v.id = o.vedtak_ref
     RIGHT JOIN tildeling t on o.event_id = t.oppgave_ref
     RIGHT JOIN saksbehandler s on t.saksbehandler_ref = s.oid
WHERE fodselsnummer = :fodselsnummer
    AND o.status = 'AvventerSaksbehandler'
ORDER BY o.opprettet DESC;
    """
    return run(queryOf(query, mapOf("fodselsnummer" to fødselsnummer.toLong())).map { row ->
        row.string("epost")
    }.asSingle)
}

