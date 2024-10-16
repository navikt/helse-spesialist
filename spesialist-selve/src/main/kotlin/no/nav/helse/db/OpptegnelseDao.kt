package no.nav.helse.db

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.abonnement.OpptegnelsePayload
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelse
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelsetype
import org.intellij.lang.annotations.Language
import java.util.UUID
import javax.sql.DataSource

class OpptegnelseDao(private val dataSource: DataSource) : OpptegnelseRepository {
    override fun opprettOpptegnelse(
        fødselsnummer: String,
        payload: OpptegnelsePayload,
        type: OpptegnelseType,
    ) {
        sessionOf(dataSource).use { session ->
            TransactionalOpptegnelseDao(session).opprettOpptegnelse(fødselsnummer, payload, type)
        }
    }

    override fun finnOpptegnelser(saksbehandlerIdent: UUID) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query =
                """
                SELECT o.sekvensnummer, p.aktor_id, o.payload, o.type
                FROM opptegnelse o
                JOIN person p ON o.person_id = p.id
                JOIN abonnement_for_opptegnelse a ON a.person_id = o.person_id
                JOIN saksbehandler_opptegnelse_sekvensnummer sos ON sos.saksbehandler_id = a.saksbehandler_id
                WHERE a.saksbehandler_id = :saksbehandlerIdent
                AND o.SEKVENSNUMMER > sos.siste_sekvensnummer
                """.trimIndent()
            session.run(
                queryOf(query, mapOf("saksbehandlerIdent" to saksbehandlerIdent))
                    .map { row ->
                        Opptegnelse(
                            payload = row.string("payload"),
                            aktorId = row.long("aktor_id").toString(),
                            sekvensnummer = row.int("sekvensnummer"),
                            type = Opptegnelsetype.valueOf(row.string("type")),
                        )
                    }.asList,
            )
        }
}
