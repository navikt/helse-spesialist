package no.nav.helse.modell.abonnement

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.Abonnement
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

internal class OpptegnelseDao(private val dataSource: DataSource) {

    internal fun finnAbonnement(saksbehandlerId: UUID) = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val statement = """
            SELECT o.saksbehandler_id, p.aktor_id, o.siste_sekvensnummer
            FROM abonnement_for_oppdatering o
            JOIN person p on o.person_id = p.id
            WHERE o.saksbehandler_id = ?;
        """
        session.run(
            queryOf(statement, saksbehandlerId)
                .map { row ->
                    Abonnement(
                        saksbehandlerId = UUID.fromString(row.string("saksbehandler_id")),
                        aktørId = row.long("aktor_id"),
                        siste_sekvensnummer = row.intOrNull("siste_sekvensnummer")
                    )
                }.asList
        )
    }

    internal fun opprettAbonnement(saksbehandlerId: UUID, aktørId: Long) = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO abonnement_for_oppdatering
            VALUES (?, (SELECT id from person where aktor_id = ?));
        """
        session.run(queryOf(statement, saksbehandlerId, aktørId).asUpdate)
    }

    internal fun oppdaterSisteSekvensnummer() {}

    internal fun opprettOpptegnelse(fødselsnummer: String, payload: UtbetalingPayload, type: OpptegnelseType) = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO opptegning (person_id, payload, type)
            VALUES ((SELECT id from person where fodselsnummer = ?), ?::jsonb, ?);
        """
        session.run(queryOf(statement, fødselsnummer.toLong(), payload.toJson(), type.toString()).asUpdate)
    }

        internal fun alleOpptegnelser() = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val statement = """
            select * from opptegning
        """
            return@using session.run(queryOf(statement).map { row ->
                OpptegnelseDto(
                    payload = row.string("payload"),
                    aktørId = row.long("person_id"),
                    sekvensnummer = row.int("sekvensnummer"),
                    type = OpptegnelseType.valueOf(row.string("type"))
                )
            }.asList)
    }

    internal fun finnOpptegnelser(saksbehandlerIdent: UUID) = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val statement = """
            SELECT o.sekvensnummer, p.aktor_id, o.payload, o.type
            FROM opptegning o
            JOIN person p on o.person_id = p.id
            join abonnement_for_oppdatering a on a.person_id = o.person_id

            WHERE a.saksbehandler_id = ?
            AND o.SEKVENSNUMMER > a.siste_sekvensnummer
            ;
        """
        session.run(
            queryOf(statement, saksbehandlerIdent)
                .map { row ->
                    OpptegnelseDto(
                        payload = row.string("payload"),
                        aktørId = row.long("aktor_id"),
                        sekvensnummer = row.int("sekvensnummer"),
                        type = OpptegnelseType.valueOf(row.string("type"))
                    )
                }.asList
        )
    }

    fun registrerSistekvensnummer(saksbehandlerIdent: UUID, sisteSekvensId: Int) = using(sessionOf(dataSource)) { session ->
        @Language("PostgreSQL")
        val statement = """
            UPDATE abonnement_for_oppdatering set siste_sekvensnummer=?
            where saksbehandler_id = ?;
        """
        session.run(queryOf(statement, sisteSekvensId, saksbehandlerIdent).asUpdate)
    }
}
