package no.nav.helse.db

import no.nav.helse.HelseDao
import no.nav.helse.spesialist.api.abonnement.OpptegnelsePayload
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType
import javax.sql.DataSource

class OpptegnelseDao(dataSource: DataSource) : HelseDao(dataSource), OpptegnelseRepository {
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
        """,
            mapOf(
                "fodselsnummer" to fødselsnummer.toLong(),
                "payload" to payload.toJson(),
                "type" to "$type",
            ),
        ).update()
    }
}
