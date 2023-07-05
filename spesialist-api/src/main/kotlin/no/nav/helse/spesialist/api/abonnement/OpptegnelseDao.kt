package no.nav.helse.spesialist.api.abonnement

import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.HelseDao
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelse
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelsetype

class OpptegnelseDao(dataSource: DataSource) : HelseDao(dataSource) {

    fun opprettOpptegnelse(fødselsnummer: String, payload: OpptegnelsePayload, type: OpptegnelseType) = asSQL(
        """
            INSERT INTO opptegnelse (person_id, payload, type)
            SELECT id, :payload::jsonb, :type
            FROM person
            WHERE fodselsnummer = :fodselsnummer
        """, mapOf(
            "fodselsnummer" to fødselsnummer.toLong(),
            "payload" to payload.toJson(),
            "type" to "$type",
        )
    ).update()

    fun finnOpptegnelser(saksbehandlerIdent: UUID) = asSQL(
        """
            SELECT o.sekvensnummer, p.aktor_id, o.payload, o.type
            FROM opptegnelse o
            JOIN person p ON o.person_id = p.id
            JOIN abonnement_for_opptegnelse a ON a.person_id = o.person_id
            WHERE a.saksbehandler_id = :saksbehandlerIdent
            AND (a.siste_sekvensnummer IS NULL OR o.SEKVENSNUMMER > a.siste_sekvensnummer)
        """, mapOf("saksbehandlerIdent" to saksbehandlerIdent)
    ).list { row ->
        Opptegnelse(
            payload = row.string("payload"),
            aktorId = row.long("aktor_id").toString(),
            sekvensnummer = row.int("sekvensnummer"),
            type = Opptegnelsetype.valueOf(row.string("type"))
        )
    }
}
