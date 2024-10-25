package no.nav.helse.db

import kotliquery.Session
import kotliquery.sessionOf
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.HelseDao.Companion.list
import no.nav.helse.HelseDao.Companion.update
import no.nav.helse.spesialist.api.abonnement.OpptegnelsePayload
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelse
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelsetype
import java.util.UUID
import javax.sql.DataSource

class OpptegnelseDao(private val session: Session) : OpptegnelseRepository {
    object NonTransactional {
        operator fun invoke(dataSource: DataSource): OpptegnelseRepository {
            fun <T> inSession(block: (Session) -> T) = sessionOf(dataSource).use { block(it) }

            return object : OpptegnelseRepository {
                override fun opprettOpptegnelse(
                    fødselsnummer: String,
                    payload: OpptegnelsePayload,
                    type: OpptegnelseType,
                ) = inSession { OpptegnelseDao(it).opprettOpptegnelse(fødselsnummer, payload, type) }

                override fun finnOpptegnelser(saksbehandlerIdent: UUID): List<Opptegnelse> {
                    return inSession { OpptegnelseDao(it).finnOpptegnelser(saksbehandlerIdent) }
                }
            }
        }
    }

    override fun opprettOpptegnelse(
        fødselsnummer: String,
        payload: OpptegnelsePayload,
        type: OpptegnelseType,
    ) {
        asSQL(
            """
            INSERT INTO opptegnelse (person_id, payload, type)
            SELECT id, :payload::jsonb, :type
            FROM person
            WHERE fodselsnummer = :fodselsnummer
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer.toLong(),
            "payload" to payload.toJson(),
            "type" to "$type",
        ).update(session)
    }

    override fun finnOpptegnelser(saksbehandlerIdent: UUID) =
        asSQL(
            """
            SELECT o.sekvensnummer, p.aktor_id, o.payload, o.type
            FROM opptegnelse o
            JOIN person p ON o.person_id = p.id
            JOIN abonnement_for_opptegnelse a ON a.person_id = o.person_id
            JOIN saksbehandler_opptegnelse_sekvensnummer sos ON sos.saksbehandler_id = a.saksbehandler_id
            WHERE a.saksbehandler_id = :saksbehandlerIdent
            AND o.SEKVENSNUMMER > sos.siste_sekvensnummer
            """.trimIndent(),
            "saksbehandlerIdent" to saksbehandlerIdent,
        ).list(session) { row ->
            Opptegnelse(
                payload = row.string("payload"),
                aktorId = row.long("aktor_id").toString(),
                sekvensnummer = row.int("sekvensnummer"),
                type = Opptegnelsetype.valueOf(row.string("type")),
            )
        }
}
