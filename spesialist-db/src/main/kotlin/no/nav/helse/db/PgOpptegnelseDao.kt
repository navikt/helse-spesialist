package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.db.HelseDao.Companion.asSQL
import java.util.UUID
import javax.sql.DataSource

class PgOpptegnelseDao private constructor(private val queryRunner: QueryRunner) : OpptegnelseDao, QueryRunner by queryRunner {
    internal constructor(session: Session) : this(MedSession(session))
    internal constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    override fun opprettOpptegnelse(
        fødselsnummer: String,
        payload: String,
        type: OpptegnelseDao.OpptegnelseType,
    ) {
        asSQL(
            """
            INSERT INTO opptegnelse (person_id, payload, type)
            SELECT id, :payload::jsonb, :type
            FROM person
            WHERE fødselsnummer = :fodselsnummer
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
            "payload" to payload,
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
            OpptegnelseDao.Opptegnelse(
                payload = row.string("payload"),
                aktorId = row.long("aktør_id").toString(),
                sekvensnummer = row.int("sekvensnummer"),
                type = OpptegnelseDao.OpptegnelseType.valueOf(row.string("type")),
            )
        }
}
