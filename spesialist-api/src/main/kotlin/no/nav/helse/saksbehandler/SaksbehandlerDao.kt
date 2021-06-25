package no.nav.helse.saksbehandler

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

class SaksbehandlerDao(private val dataSource: DataSource) {
    fun opprettSaksbehandler(
        oid: UUID,
        navn: String,
        epost: String,
        ident: String,
    ) = sessionOf(dataSource).use {
        @Language("PostgreSQL")
        val opprettSaksbehandlerQuery = """
            INSERT INTO saksbehandler(oid, navn, epost, ident) VALUES (:oid, :navn, :epost, :ident)
            ON CONFLICT (oid)
                DO UPDATE SET navn = :navn, epost = :epost, ident = :ident
                WHERE (saksbehandler.navn, saksbehandler.epost, saksbehandler.ident) IS DISTINCT FROM
                    (excluded.navn, excluded.epost, excluded.ident)
        """
        it.run(
            queryOf(
                opprettSaksbehandlerQuery,
                mapOf<String, Any>("oid" to oid, "navn" to navn, "epost" to epost, "ident" to ident
                )
            ).asUpdate
        )
    }

    fun finnSaksbehandler(oid: UUID) = sessionOf(dataSource).use {
        @Language("PostgreSQL")
        val query = """ SELECT * FROM saksbehandler WHERE oid = ? LIMIT 1"""
        it.run(queryOf(query, oid).map { row ->
            SaksbehandlerDto(
                oid = oid,
                navn = row.string("navn"),
                epost = row.string("epost"),
                ident = row.string("ident"),
            )
        }.asSingle)
    }

    fun finnSaksbehandler(epost: String) = sessionOf(dataSource).use {
        @Language("PostgreSQL")
        val query = """ SELECT * FROM saksbehandler WHERE epost = ? LIMIT 1"""
        it.run(queryOf(query, epost).map { row ->
            SaksbehandlerDto(
                oid = UUID.fromString(row.string("oid")),
                navn = row.string("navn"),
                epost = row.string("epost"),
                ident = row.string("ident")
            )
        }.asSingle)
    }

    fun invaliderSaksbehandleroppgaver(fødselsnummer: String, orgnummer: String) =
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

