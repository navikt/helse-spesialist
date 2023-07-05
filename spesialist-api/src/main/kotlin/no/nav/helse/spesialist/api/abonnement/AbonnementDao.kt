package no.nav.helse.spesialist.api.abonnement

import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.HelseDao

class AbonnementDao(dataSource: DataSource) : HelseDao(dataSource) {

    fun opprettAbonnement(saksbehandlerId: UUID, aktørId: Long) =
        asSQL(
            """
                INSERT INTO abonnement_for_opptegnelse
                SELECT :saksbehandlerId, p.id, MAX(o.sekvensnummer) FROM person p
                LEFT JOIN opptegnelse o on p.id = o.person_id
                WHERE p.aktor_id = :aktorId
                GROUP BY p.id
                ON CONFLICT DO NOTHING;
      """, mapOf("saksbehandlerId" to saksbehandlerId, "aktorId" to aktørId)
        ).update()

    fun registrerSistekvensnummer(saksbehandlerIdent: UUID, sisteSekvensId: Int) = asSQL(
        """ UPDATE abonnement_for_opptegnelse
            SET siste_sekvensnummer=:sisteSekvensId
            WHERE saksbehandler_id=:saksbehandlerId; """,
        mapOf("sisteSekvensId" to sisteSekvensId, "saksbehandlerId" to saksbehandlerIdent)
    ).update()
}
