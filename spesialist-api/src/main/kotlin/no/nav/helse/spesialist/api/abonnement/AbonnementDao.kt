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

            val sekvensnummerQuery = asSQL(
                """
                    -- hmm, kanskje vi egentlig bare burde sette max(sekvensnummer), ikke joine inn aktuell person?
                    with aktuelt_sekvensnummer as (
                        -- Forklaring: høyeste for person, dernest høyeste globalt, dernest 0
                        select coalesce(max(sekvensnummer), (select max(sekvensnummer) from opptegnelse), 0) sekvensnummeret
                        from opptegnelse o
                        join person p on o.person_id = p.id
                        where aktor_id = :aktorId
                    )
                    
                    insert into saksbehandler_opptegnelse_sekvensnummer
                    select :saksbehandlerId, (select sekvensnummeret from aktuelt_sekvensnummer)
                    from person p
                    where aktor_id = :aktorId
                    on conflict (saksbehandler_id) do update
                        set siste_sekvensnummer = (select sekvensnummeret from aktuelt_sekvensnummer)
                """.trimIndent(), mapOf("saksbehandlerId" to saksbehandlerId, "aktorId" to aktørId)
            )
            transactionalSession.run(sekvensnummerQuery.asUpdate)
        }
    }

    fun registrerSistekvensnummer(saksbehandlerIdent: UUID, sisteSekvensId: Int) = asSQL(
        """
            update saksbehandler_opptegnelse_sekvensnummer
            set siste_sekvensnummer = :sisteSekvensId
            where saksbehandler_id = :saksbehandlerId;
        """.trimIndent(), mapOf("sisteSekvensId" to sisteSekvensId, "saksbehandlerId" to saksbehandlerIdent)
    ).update()
}
