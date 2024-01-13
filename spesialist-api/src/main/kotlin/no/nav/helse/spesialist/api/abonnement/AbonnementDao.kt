package no.nav.helse.spesialist.api.abonnement

import java.util.UUID
import javax.sql.DataSource
import kotliquery.sessionOf
import no.nav.helse.HelseDao

class AbonnementDao(private val dataSource: DataSource) : HelseDao(dataSource) {

    fun opprettAbonnement(saksbehandlerId: UUID, aktørId: Long) = sessionOf(dataSource).use { session ->
        session.transaction { transactionalSession ->
            val abonnementQuery = asSQL(
                """
                    delete from abonnement_for_opptegnelse where saksbehandler_id = :saksbehandlerId;
                    insert into abonnement_for_opptegnelse
                    select :saksbehandlerId, p.id
                    from person p
                    where p.aktor_id = :aktorId
                """.trimIndent(), mapOf("saksbehandlerId" to saksbehandlerId, "aktorId" to aktørId)
            )
            transactionalSession.run(abonnementQuery.asUpdate)

            if (saksbehandlerHarSekvensnummer(saksbehandlerId)) return@transaction

            val sekvensnummerQuery = asSQL(
                """
                    insert into saksbehandler_opptegnelse_sekvensnummer
                    select :saksbehandlerId, coalesce(max(o.sekvensnummer), (select max(sekvensnummer) from opptegnelse), 0)
                    from opptegnelse o
                             join person p on o.person_id = p.id
                    where aktor_id = :aktorId
                """.trimIndent(), mapOf("saksbehandlerId" to saksbehandlerId, "aktorId" to aktørId)
            )
            transactionalSession.run(sekvensnummerQuery.asUpdate)
        }
    }

    private fun saksbehandlerHarSekvensnummer(saksbehandlerIdent: UUID) = asSQL(
        """
            select siste_sekvensnummer
            from saksbehandler_opptegnelse_sekvensnummer
            where saksbehandler_id = :saksbehandlerId;
        """.trimIndent(), mapOf("saksbehandlerId" to saksbehandlerIdent)
    ).list { it }.isNotEmpty()

    fun registrerSistekvensnummer(saksbehandlerIdent: UUID, sisteSekvensId: Int) = asSQL(
        """
            update saksbehandler_opptegnelse_sekvensnummer
            set siste_sekvensnummer = :sisteSekvensId
            where saksbehandler_id = :saksbehandlerId;
        """.trimIndent(), mapOf("sisteSekvensId" to sisteSekvensId, "saksbehandlerId" to saksbehandlerIdent)
    ).update()
}
