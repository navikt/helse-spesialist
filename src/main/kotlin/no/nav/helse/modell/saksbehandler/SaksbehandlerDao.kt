package no.nav.helse.modell.saksbehandler

import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

internal class SaksbehandlerDao(private val dataSource: DataSource) {
    internal fun opprettSaksbehandler(
        oid: UUID,
        navn: String,
        epost: String
    ) = sessionOf(dataSource).use {
        it.persisterSaksbehandler(oid, navn, epost)
    }

    internal fun finnSaksbehandler(oid: UUID) = sessionOf(dataSource).use {
        it.finnSaksbehandler(oid)
    }

    internal fun invaliderSaksbehandleroppgaver(fødselsnummer: String, orgnummer: String) =
        sessionOf(dataSource).use { it.invaliderSaksbehandleroppgaver(fødselsnummer, orgnummer) }

    internal fun persisterSaksbehandler(
        oid: UUID,
        navn: String,
        epost: String
    ) = using(sessionOf(dataSource)) { it.persisterSaksbehandler(oid, navn, epost) }

    private fun Session.persisterSaksbehandler(
        oid: UUID,
        navn: String,
        epost: String
    ) {
        @Language("PostgreSQL")
        val opprettSaksbehandlerQuery = """
        INSERT INTO saksbehandler(oid, navn, epost)
        VALUES (:oid,
                :navn,
                :epost)
        ON CONFLICT DO NOTHING
    """

        run(
            queryOf(
                opprettSaksbehandlerQuery,
                mapOf(
                    "oid" to oid,
                    "navn" to navn,
                    "epost" to epost
                )
            ).asUpdate
        )
    }

    private fun Session.finnSaksbehandler(oid: UUID): List<SaksbehandlerDto> {
        @Language("PostgreSQL")
        val finnSaksbehandlerQuery = """
SELECT *
FROM saksbehandler
WHERE oid = ?
    """
        return this.run(queryOf(finnSaksbehandlerQuery, oid).map { saksbehandlerRow ->
            val oid = saksbehandlerRow.string("oid")

            SaksbehandlerDto(
                oid = UUID.fromString(oid),
                navn = saksbehandlerRow.string("navn"),
                epost = saksbehandlerRow.string("epost")
            )
        }.asList)
    }

    private fun Session.invaliderSaksbehandleroppgaver(fødselsnummer: String, orgnummer: String) {
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

        run(
            queryOf(
                finnOppgaveIder,
                mapOf("orgnummer" to orgnummer.toLong(), "fodselsnummer" to fødselsnummer.toLong())
            ).map { it.long("id") }.asList
        ).forEach { id -> run(queryOf(invaliderOppgave, mapOf("id" to id)).asUpdate) }
    }
}

data class SaksbehandlerDto(
    val oid: UUID,
    val navn: String,
    val epost: String
)
