package no.nav.helse.db

import kotliquery.Session
import kotliquery.queryOf
import no.nav.helse.spesialist.api.abonnement.OpptegnelsePayload
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelse
import org.intellij.lang.annotations.Language
import java.util.UUID
import javax.naming.OperationNotSupportedException

class TransactionalOpptegnelseDao(private val session: Session) : OpptegnelseRepository {
    override fun opprettOpptegnelse(
        fødselsnummer: String,
        payload: OpptegnelsePayload,
        type: OpptegnelseType,
    ) {
        @Language("PostgreSQL")
        val statement =
            """
            INSERT INTO opptegnelse (person_id, payload, type)
            SELECT id, :payload::jsonb, :type
            FROM person
            WHERE fodselsnummer = :fodselsnummer
            """.trimIndent()
        session.run(
            queryOf(
                statement,
                mapOf(
                    "fodselsnummer" to fødselsnummer.toLong(),
                    "payload" to payload.toJson(),
                    "type" to "$type",
                ),
            ).asUpdate,
        )
    }

    override fun finnOpptegnelser(saksbehandlerIdent: UUID): List<Opptegnelse> = throw OperationNotSupportedException()
}
