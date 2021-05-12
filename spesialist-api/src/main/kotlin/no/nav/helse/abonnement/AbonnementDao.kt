package no.nav.helse.abonnement

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

class AbonnementDao(private val dataSource: DataSource) {

    fun opprettAbonnement(saksbehandlerId: UUID, aktørId: Long) = sessionOf(dataSource).use  { session ->
        @Language("PostgreSQL")
        val statement = """
            INSERT INTO abonnement_for_opptegnelse
                SELECT ?, p.id, MAX(o.sekvensnummer) FROM person p LEFT JOIN opptegnelse o on p.id = o.person_id WHERE p.aktor_id = ? GROUP BY p.id
            ON CONFLICT DO NOTHING;
        """
        session.run(queryOf(statement, saksbehandlerId, aktørId).asUpdate)
    }

    fun registrerSistekvensnummer(saksbehandlerIdent: UUID, sisteSekvensId: Int) =
        sessionOf(dataSource).use  { session ->
            @Language("PostgreSQL")
            val statement = """
                UPDATE abonnement_for_opptegnelse
                SET siste_sekvensnummer=?
                WHERE saksbehandler_id=?;
            """
            session.run(queryOf(statement, sisteSekvensId, saksbehandlerIdent).asUpdate)
        }
}
