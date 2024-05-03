package no.nav.helse.spesialist.api.abonnement

import kotliquery.sessionOf
import no.nav.helse.HelseDao
import java.util.UUID
import javax.sql.DataSource

class AbonnementDao(private val dataSource: DataSource) : HelseDao(dataSource) {
    fun opprettAbonnement(
        saksbehandlerId: UUID,
        aktørId: Long,
    ) = sessionOf(dataSource).use { session ->
        session.transaction { transactionalSession ->
            val abonnementQuery =
                asSQL(
                    """
                    delete from abonnement_for_opptegnelse where saksbehandler_id = :saksbehandlerId;
                    insert into abonnement_for_opptegnelse
                    select :saksbehandlerId, p.id
                    from person p
                    where p.aktor_id = :aktorId
                    """.trimIndent(),
                    mapOf("saksbehandlerId" to saksbehandlerId, "aktorId" to aktørId),
                )
            transactionalSession.run(abonnementQuery.asUpdate)

            val sekvensnummerQuery =
                asSQL(
                    """
                    insert into saksbehandler_opptegnelse_sekvensnummer
                    values (:saksbehandlerId, coalesce((select max(sekvensnummer) from opptegnelse), 0))
                    on conflict (saksbehandler_id) do update
                        set siste_sekvensnummer = (coalesce((select max(sekvensnummer) from opptegnelse), 0))
                    """.trimIndent(),
                    mapOf("saksbehandlerId" to saksbehandlerId, "aktorId" to aktørId),
                )
            transactionalSession.run(sekvensnummerQuery.asUpdate)
        }
    }

    fun registrerSistekvensnummer(
        saksbehandlerIdent: UUID,
        sisteSekvensId: Int,
    ) = asSQL(
        """
        update saksbehandler_opptegnelse_sekvensnummer
        set siste_sekvensnummer = :sisteSekvensId
        where saksbehandler_id = :saksbehandlerId;
        """.trimIndent(),
        mapOf("sisteSekvensId" to sisteSekvensId, "saksbehandlerId" to saksbehandlerIdent),
    ).update()
}
