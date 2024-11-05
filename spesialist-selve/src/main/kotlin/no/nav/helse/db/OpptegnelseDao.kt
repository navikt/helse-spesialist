package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.api.abonnement.OpptegnelsePayload
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelse
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelsetype
import java.util.UUID
import javax.sql.DataSource

class OpptegnelseDao(private val queryRunner: QueryRunner) : OpptegnelseRepository, QueryRunner by queryRunner {
    constructor(session: Session) : this(MedSession(session))
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

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
            WHERE fødselsnummer = :fodselsnummer
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
            "payload" to payload.toJson(),
            "type" to "$type",
        ).update()
    }

    override fun finnOpptegnelser(saksbehandlerIdent: UUID) =
        asSQL(
            """
            SELECT o.sekvensnummer, p.aktør_id, o.payload, o.type
            FROM opptegnelse o
            JOIN person p ON o.person_id = p.id
            JOIN abonnement_for_opptegnelse a ON a.person_id = o.person_id
            JOIN saksbehandler_opptegnelse_sekvensnummer sos ON sos.saksbehandler_id = a.saksbehandler_id
            WHERE a.saksbehandler_id = :saksbehandlerIdent
            AND o.SEKVENSNUMMER > sos.siste_sekvensnummer
            """.trimIndent(),
            "saksbehandlerIdent" to saksbehandlerIdent,
        ).list { row ->
            Opptegnelse(
                payload = row.string("payload"),
                aktorId = row.long("aktør_id").toString(),
                sekvensnummer = row.int("sekvensnummer"),
                type = Opptegnelsetype.valueOf(row.string("type")),
            )
        }
}
