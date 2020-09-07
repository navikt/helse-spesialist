package no.nav.helse.tildeling

import kotliquery.Session
import kotliquery.queryOf
import org.intellij.lang.annotations.Language
import java.util.*

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

fun Session.hentSaksbehandlerFor(oppgaveReferanse: UUID): String? {
    @Language("PostgreSQL")
    val query =
        "SELECT * FROM tildeling INNER JOIN saksbehandler s on s.oid = tildeling.saksbehandler_ref WHERE oppgave_ref=:oppgave_ref;"
    return run(queryOf(query, mapOf("oppgave_ref" to oppgaveReferanse)).map { row ->
        row.string("epost")
    }.asSingle)
}

fun Session.slettOppgavetildeling(oppgaveReferanse: UUID) {
    @Language("PostgreSQL")
    val query = "DELETE FROM tildeling WHERE oppgave_ref=:oppgave_ref;"
    run(queryOf(query, mapOf("oppgave_ref" to oppgaveReferanse)).asUpdate)
}
