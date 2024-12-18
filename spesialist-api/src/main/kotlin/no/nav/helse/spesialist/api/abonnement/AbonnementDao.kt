package no.nav.helse.spesialist.api.abonnement

import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.QueryRunner
import java.util.UUID
import javax.sql.DataSource

class AbonnementDao(dataSource: DataSource) : QueryRunner by MedDataSource(dataSource) {
    fun opprettAbonnement(
        saksbehandlerId: UUID,
        aktørId: String,
    ) {
        lagre(saksbehandlerId, aktørId)
        upsertSekvensnummer(saksbehandlerId)
    }

    fun registrerSistekvensnummer(
        saksbehandlerIdent: UUID,
        sisteSekvensId: Int,
    ): Int =
        asSQL(
            """
            update saksbehandler_opptegnelse_sekvensnummer
            set siste_sekvensnummer = :sisteSekvensId
            where saksbehandler_id = :saksbehandlerId;
            """.trimIndent(),
            "sisteSekvensId" to sisteSekvensId,
            "saksbehandlerId" to saksbehandlerIdent,
        ).update()

    private fun upsertSekvensnummer(saksbehandlerId: UUID) {
        asSQL(
            """
            insert into saksbehandler_opptegnelse_sekvensnummer
            values (:saksbehandlerId, coalesce((select max(sekvensnummer) from opptegnelse), 0))
            on conflict (saksbehandler_id) do update
                set siste_sekvensnummer = (coalesce((select max(sekvensnummer) from opptegnelse), 0))
            """.trimIndent(),
            "saksbehandlerId" to saksbehandlerId,
        ).update()
    }

    private fun lagre(
        saksbehandlerId: UUID,
        aktørId: String,
    ) {
        asSQL(
            """
            delete from abonnement_for_opptegnelse where saksbehandler_id = :saksbehandlerId;
            insert into abonnement_for_opptegnelse
            select :saksbehandlerId, p.id
            from person p
            where p.aktør_id = :aktorId
            """.trimIndent(),
            "saksbehandlerId" to saksbehandlerId,
            "aktorId" to aktørId,
        ).update()
    }
}
