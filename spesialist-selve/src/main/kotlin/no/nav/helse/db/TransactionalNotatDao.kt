package no.nav.helse.db

import kotliquery.Session
import kotliquery.queryOf
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import org.intellij.lang.annotations.Language
import java.util.UUID

class TransactionalNotatDao(
    private val session: Session,
) : NotatRepository {
    override fun lagreForOppgaveId(
        oppgaveId: Long,
        tekst: String,
        saksbehandlerOid: UUID,
        notatType: NotatType,
    ): Long? {
        @Language("PostgreSQL")
        val statement =
            """
            INSERT INTO notat (vedtaksperiode_id, tekst, saksbehandler_oid, type)
            VALUES (
                (SELECT v.vedtaksperiode_id 
                    FROM vedtak v 
                    INNER JOIN oppgave o on v.id = o.vedtak_ref 
                    WHERE o.id = :oppgave_id), 
                :tekst, 
                :saksbehandler_oid,
                CAST(:type as notattype)
            );            
            """.trimIndent()
        return session.run(
            queryOf(
                statement,
                mapOf(
                    "oppgave_id" to oppgaveId,
                    "tekst" to tekst,
                    "saksbehandler_oid" to saksbehandlerOid,
                    "type" to notatType.name,
                ),
            ).asUpdateAndReturnGeneratedKey,
        )
    }
}
