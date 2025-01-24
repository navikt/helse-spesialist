package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.db.HelseDao.Companion.asSQL
import no.nav.helse.db.HelseDao.Companion.single
import java.util.UUID

class PgMetrikkDao internal constructor(private val session: Session) : MetrikkDao {
    override fun finnUtfallForGodkjenningsbehov(contextId: UUID): GodkjenningsbehovUtfall {
        val antallSuspenderinger =
            asSQL(
                """
                select count(*) from command_context
                where context_id = :contextId
                and tilstand = 'SUSPENDERT'
                """.trimIndent(),
                "contextId" to contextId,
            ).single(session) { row -> row.int(1) }!!

        // Hvis kommandokjeden ikke ble suspendert anser vi at behandlingen av godkjenningsbehovet ble avbrutt.
        // Ja, det er passe shaky 🫠
        if (antallSuspenderinger == 0) return GodkjenningsbehovUtfall.Avbrutt

        val bleAutomatiskGodkjent =
            asSQL(
                """
            select distinct automatisert from automatisering
            join command_context cc on automatisering.hendelse_ref = cc.hendelse_id
            where context_id =  :contextId
            """,
                "contextId" to contextId,
            ).single(session) { row -> row.boolean("automatisert") } ?: false

        if (bleAutomatiskGodkjent) return GodkjenningsbehovUtfall.AutomatiskGodkjent

        val gikkTilManuell =
            asSQL(
                """
            select distinct 1
            from oppgave o
            join command_context cc on o.hendelse_id_godkjenningsbehov = cc.hendelse_id
            where cc.context_id = :contextId
            """,
                "contextId" to contextId,
            ).single(session) { true } ?: false

        return if (gikkTilManuell) {
            GodkjenningsbehovUtfall.ManuellOppgave
        } else {
            GodkjenningsbehovUtfall.AutomatiskAvvist
        }
    }
}
