package no.nav.helse.abonnement

import no.nav.helse.HelseDao
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

class OpptegnelseDao(dataSource: DataSource) : HelseDao(dataSource) {

    @Language("PostgreSQL")
    fun opprettOpptegnelse(fødselsnummer: String, payload: OpptegnelsePayload, type: OpptegnelseType) =
        """
            INSERT INTO opptegnelse (person_id, payload, type)
            VALUES ((SELECT id FROM person WHERE fodselsnummer=:fodselsnummer), cast(:payload as jsonb), :type);
            """.update(mapOf("fodselsnummer" to fødselsnummer.toLong(), "payload" to payload.toJson(), "type" to "$type"))

    @Language("PostgreSQL")
    fun finnOpptegnelser(saksbehandlerIdent: UUID) =
        """
            SELECT o.sekvensnummer, p.aktor_id, o.payload, o.type
            FROM opptegnelse o
            JOIN person p ON o.person_id = p.id
            JOIN abonnement_for_opptegnelse a ON a.person_id = o.person_id

            WHERE a.saksbehandler_id= :saksbehandlerIdent
            AND (
            a.siste_sekvensnummer IS NULL
            OR o.SEKVENSNUMMER > a.siste_sekvensnummer
            );
        """.list(mapOf("saksbehandlerIdent" to saksbehandlerIdent)) { row ->
            OpptegnelseDto(
                payload = row.string("payload"),
                aktørId = row.long("aktor_id"),
                sekvensnummer = row.int("sekvensnummer"),
                type = OpptegnelseType.valueOf(row.string("type"))
            )
        }
}
