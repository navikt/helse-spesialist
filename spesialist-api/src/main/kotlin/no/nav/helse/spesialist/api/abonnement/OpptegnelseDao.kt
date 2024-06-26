package no.nav.helse.spesialist.api.abonnement

import no.nav.helse.HelseDao
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelse
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelsetype
import java.util.UUID
import javax.sql.DataSource

class OpptegnelseDao(dataSource: DataSource) : HelseDao(dataSource) {
    fun opprettOpptegnelse(
        fødselsnummer: String,
        payload: OpptegnelsePayload,
        type: OpptegnelseType,
    ) = asSQL(
        """
            INSERT INTO opptegnelse (person_id, payload, type)
            SELECT id, :payload::jsonb, :type
            FROM person
            WHERE fodselsnummer = :fodselsnummer
        """,
        mapOf(
            "fodselsnummer" to fødselsnummer.toLong(),
            "payload" to payload.toJson(),
            "type" to "$type",
        ),
    ).update()

    fun finnOpptegnelser(saksbehandlerIdent: UUID) =
        asSQL(
            """
            SELECT o.sekvensnummer, p.aktor_id, o.payload, o.type
            FROM opptegnelse o
            JOIN person p ON o.person_id = p.id
            JOIN abonnement_for_opptegnelse a ON a.person_id = o.person_id
            JOIN saksbehandler_opptegnelse_sekvensnummer sos ON sos.saksbehandler_id = a.saksbehandler_id
            WHERE a.saksbehandler_id = :saksbehandlerIdent
            AND o.SEKVENSNUMMER > sos.siste_sekvensnummer
        """,
            mapOf("saksbehandlerIdent" to saksbehandlerIdent),
        ).list { row ->
            Opptegnelse(
                payload = row.string("payload"),
                aktorId = row.long("aktor_id").toString(),
                sekvensnummer = row.int("sekvensnummer"),
                type = Opptegnelsetype.valueOf(row.string("type")),
            )
        }
}
