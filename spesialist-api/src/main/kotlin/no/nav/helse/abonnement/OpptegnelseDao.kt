package no.nav.helse.abonnement

import no.nav.helse.HelseDao
import java.util.*
import javax.sql.DataSource

class OpptegnelseDao(dataSource: DataSource) : HelseDao(dataSource) {

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
