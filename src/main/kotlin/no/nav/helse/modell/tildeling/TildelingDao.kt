package no.nav.helse.modell.tildeling

import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

internal class TildelingDao(private val dataSource: DataSource) {
    internal fun opprettTildeling(oppgaveId: Long, saksbehandleroid: UUID, gyldigTil: LocalDateTime? = null) {
        sessionOf(dataSource).use {
            it.tildelOppgave(oppgaveId, saksbehandleroid, gyldigTil)
        }
    }

    internal fun slettTildeling(oppgaveId: Long) {
        sessionOf(dataSource).use { session ->
            session.slettOppgavetildeling(oppgaveId)
        }
    }

    internal fun fjernPåVent(oppgaveId: Long) {
        sessionOf(dataSource).use { session ->
            session.fjernPåVent(oppgaveId)
        }
    }

    internal fun finnSaksbehandlerNavn(oppgaveId: Long) = sessionOf(dataSource).use { session ->
        session.hentSaksbehandlerNavnFor(oppgaveId)
    }

    internal fun finnSaksbehandlerEpost(oppgaveId: Long) = sessionOf(dataSource)
        .use { it.hentSaksbehandlerEpostFor(oppgaveId) }

    internal fun tildelingForPerson(fødselsnummer: String) = sessionOf(dataSource)
        .use { it.tildelingForPerson(fødselsnummer) }

    internal fun tildelOppgave(oppgaveId: Long, saksbehandleroid: UUID, gyldigTil: LocalDateTime? = null) =
        using(sessionOf(dataSource)) { it.tildelOppgave(oppgaveId, saksbehandleroid, gyldigTil) }

    internal fun leggOppgavePåVent(oppgaveId: Long) {
        sessionOf(dataSource).use {
            it.leggOppgavePåVent(oppgaveId)
        }
    }

    private fun Session.tildelOppgave(oppgaveId: Long, saksbehandleroid: UUID, gyldigTil: LocalDateTime? = null) {
        @Language("PostgreSQL")
        val query =
            "INSERT INTO tildeling(oppgave_id_ref, saksbehandler_ref, gyldig_til) VALUES(:oppgave_id_ref, :saksbehandler_ref, :gyldig_til);"
        run(
            queryOf(
                query, mapOf(
                    "oppgave_id_ref" to oppgaveId,
                    "saksbehandler_ref" to saksbehandleroid,
                    "gyldig_til" to gyldigTil
                )
            ).asUpdate
        )
    }

    private fun Session.hentSaksbehandlerEpostFor(oppgaveId: Long): String? {
        @Language("PostgreSQL")
        val query =
            "SELECT * FROM tildeling INNER JOIN saksbehandler s on s.oid = tildeling.saksbehandler_ref WHERE oppgave_id_ref=:oppgave_id_ref AND (gyldig_til IS NULL OR gyldig_til > now());"
        return run(queryOf(query, mapOf("oppgave_id_ref" to oppgaveId)).map { row ->
            row.string("epost")
        }.asSingle)
    }

    private fun Session.hentSaksbehandlerNavnFor(oppgaveId: Long): String? {
        @Language("PostgreSQL")
        val query =
            "SELECT * FROM tildeling INNER JOIN saksbehandler s on s.oid = tildeling.saksbehandler_ref WHERE oppgave_id_ref=:oppgave_id_ref AND (gyldig_til IS NULL OR gyldig_til > now());"
        return run(queryOf(query, mapOf("oppgave_id_ref" to oppgaveId)).map { row ->
            row.string("navn")
        }.asSingle)
    }

    private fun Session.slettOppgavetildeling(oppgaveId: Long) {
        @Language("PostgreSQL")
        val query = "DELETE FROM tildeling WHERE oppgave_id_ref=:oppgave_id_ref;"
        run(queryOf(query, mapOf(
            "oppgave_id_ref" to oppgaveId)).asUpdate
        )
    }

    private fun Session.fjernPåVent(oppgaveId: Long) {
        @Language("PostgreSQL")
        val query = "UPDATE tildeling SET på_vent = false WHERE oppgave_id_ref=:oppgave_id_ref;"
        run(queryOf(query, mapOf("oppgave_id_ref" to oppgaveId)).asUpdate)
    }

    private fun Session.tildelingForPerson(fødselsnummer: String): String? {
        @Language("PostgreSQL")
        val query = """
            SELECT s.epost FROM person
                 RIGHT JOIN vedtak v on person.id = v.person_ref
                 RIGHT JOIN oppgave o on v.id = o.vedtak_ref
                 RIGHT JOIN tildeling t on o.id = t.oppgave_id_ref AND (t.gyldig_til IS NULL OR t.gyldig_til > now())
                 RIGHT JOIN saksbehandler s on t.saksbehandler_ref = s.oid
            WHERE fodselsnummer = :fodselsnummer
                AND o.status = 'AvventerSaksbehandler'
            ORDER BY o.opprettet DESC;
        """
        return run(queryOf(query, mapOf("fodselsnummer" to fødselsnummer.toLong())).map { row ->
            row.string("epost")
        }.asSingle)
    }

    private fun Session.leggOppgavePåVent(oppgaveId: Long) {
        @Language("PostgreSQL")
        val query = "UPDATE tildeling SET på_vent = true WHERE oppgave_id_ref = :oppgave_id_ref;"
        run(
            queryOf(
                query, mapOf(
                    "oppgave_id_ref" to oppgaveId,
                )
            ).asUpdate
        )
    }
}
