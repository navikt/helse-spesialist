package no.nav.helse.modell.saksbehandler

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

internal class SaksbehandlerDao(private val dataSource: DataSource) {
    internal fun opprettSaksbehandler(
        oid: UUID,
        navn: String,
        epost: String
    ) = sessionOf(dataSource).use {
        @Language("PostgreSQL")
        val opprettSaksbehandlerQuery = """
            INSERT INTO saksbehandler(oid, navn, epost) VALUES (:oid, :navn, :epost)
            ON CONFLICT (oid)
                DO UPDATE SET navn = :navn, epost = :epost
                WHERE (saksbehandler.navn, saksbehandler.epost) IS DISTINCT FROM (excluded.navn, excluded.epost)
        """
        it.run(
            queryOf(
                opprettSaksbehandlerQuery,
                mapOf<String, Any>("oid" to oid, "navn" to navn, "epost" to epost
                )
            ).asUpdate
        )
    }

    internal fun finnSaksbehandler(oid: UUID) = sessionOf(dataSource).use {
        @Language("PostgreSQL")
        val query = """ SELECT * FROM saksbehandler WHERE oid = ? LIMIT 1"""
        it.run(queryOf(query, oid).map { row ->
            SaksbehandlerDto(
                oid = oid,
                navn = row.string("navn"),
                epost = row.string("epost")
            )
        }.asSingle)
    }

    internal fun finnSaksbehandler(epost: String) = sessionOf(dataSource).use {
        @Language("PostgreSQL")
        val query = """ SELECT * FROM saksbehandler WHERE epost = ? LIMIT 1"""
        it.run(queryOf(query, epost).map { row ->
            SaksbehandlerDto(
                oid = UUID.fromString(row.string("oid")),
                navn = row.string("navn"),
                epost = row.string("epost")
            )
        }.asSingle)
    }

    internal fun invaliderSaksbehandleroppgaver(fødselsnummer: String, orgnummer: String) =
        sessionOf(dataSource).use {
            @Language("PostgreSQL")
            val finnOppgaveIder = """
                SELECT o.*
                FROM vedtak v
                         JOIN oppgave o ON o.vedtak_ref = v.id
                         JOIN person p ON v.person_ref = p.id
                         JOIN arbeidsgiver a ON v.arbeidsgiver_ref = a.id
                WHERE a.orgnummer = :orgnummer
                  AND p.fodselsnummer = :fodselsnummer
                  AND o.status = 'AvventerSaksbehandler'::oppgavestatus;
            """

            @Language("PostgreSQL")
            val invaliderOppgave = "UPDATE oppgave SET status = 'Invalidert'::oppgavestatus WHERE id=:id;"
            it.run(
                queryOf(
                    finnOppgaveIder,
                    mapOf("orgnummer" to orgnummer.toLong(), "fodselsnummer" to fødselsnummer.toLong())
                ).map { it.long("id") }.asList
            ).forEach { id -> it.run(queryOf(invaliderOppgave, mapOf("id" to id)).asUpdate) }
        }

}

data class SaksbehandlerDto(
    val oid: UUID,
    val navn: String,
    val epost: String
)
