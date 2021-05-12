package no.nav.helse.abonnement

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

class OpptegnelseDao(private val dataSource: DataSource) {

    internal fun alleOpptegnelser() = sessionOf(dataSource).use  { session ->
        @Language("PostgreSQL")
        val statement = """
            SELECT * FROM opptegnelse;
        """
        return@use session.run(queryOf(statement).map { row ->
            OpptegnelseDto(
                payload = row.string("payload"),
                aktørId = row.long("person_id"),
                sekvensnummer = row.int("sekvensnummer"),
                type = OpptegnelseType.valueOf(row.string("type"))
            )
        }.asList)
    }

    fun finnOpptegnelser(saksbehandlerIdent: UUID) = sessionOf(dataSource).use  { session ->
        @Language("PostgreSQL")
        val statement = """
            SELECT o.sekvensnummer, p.aktor_id, o.payload, o.type
            FROM opptegnelse o
            JOIN person p ON o.person_id = p.id
            JOIN abonnement_for_opptegnelse a ON a.person_id = o.person_id

            WHERE a.saksbehandler_id=?
            AND (
            a.siste_sekvensnummer IS NULL
            OR o.SEKVENSNUMMER > a.siste_sekvensnummer
            );
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
}
