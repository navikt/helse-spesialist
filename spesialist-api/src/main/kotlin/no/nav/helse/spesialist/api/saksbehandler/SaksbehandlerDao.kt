package no.nav.helse.spesialist.api.saksbehandler

import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.HelseDao
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

class SaksbehandlerDao(private val dataSource: DataSource): HelseDao(dataSource) {
    fun opprettSaksbehandler(oid: UUID, navn: String, epost: String, ident: String) =
        """ INSERT INTO saksbehandler(oid, navn, epost, ident) VALUES (:oid, :navn, :epost, :ident)
            ON CONFLICT (oid)
                DO UPDATE SET navn = :navn, epost = :epost, ident = :ident
                WHERE (saksbehandler.navn, saksbehandler.epost, saksbehandler.ident) IS DISTINCT FROM
                    (excluded.navn, excluded.epost, excluded.ident)
        """.update(mapOf("oid" to oid, "navn" to navn, "epost" to epost, "ident" to ident))

    fun finnSaksbehandler(oid: UUID) =
        """ SELECT * FROM saksbehandler WHERE oid = :oid LIMIT 1"""
            .single(mapOf("oid" to oid)) { row ->
                SaksbehandlerDto(
                    oid = oid,
                    navn = row.string("navn"),
                    epost = row.string("epost"),
                    ident = row.string("ident"))}

    fun finnSaksbehandler(epost: String) =
        """ SELECT * FROM saksbehandler WHERE epost = :epost LIMIT 1"""
            .single(mapOf("epost" to epost)) { row ->
            SaksbehandlerDto(
                oid = UUID.fromString(row.string("oid")),
                navn = row.string("navn"),
                epost = row.string("epost"),
                ident = row.string("ident"))}

    fun invaliderSaksbehandleroppgaver(fødselsnummer: String) =
        sessionOf(dataSource).use { session: Session ->
            @Language("PostgreSQL")
            val finnOppgaveIder = """
                SELECT o.*
                FROM vedtak v
                         JOIN oppgave o ON o.vedtak_ref = v.id
                         JOIN person p ON v.person_ref = p.id
                         JOIN arbeidsgiver a ON v.arbeidsgiver_ref = a.id
                WHERE p.fodselsnummer = :fodselsnummer
                  AND o.status = 'AvventerSaksbehandler'::oppgavestatus;
            """

            @Language("PostgreSQL")
            val invaliderOppgave = "UPDATE oppgave SET status = 'Invalidert'::oppgavestatus WHERE id=:id;"
            session.run(
                queryOf(
                    finnOppgaveIder,
                    mapOf("fodselsnummer" to fødselsnummer.toLong())
                ).map { it.long("id") }.asList
            ).forEach { id -> session.run(queryOf(invaliderOppgave, mapOf("id" to id)).asUpdate) }
        }
}

