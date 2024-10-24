package no.nav.helse.mediator

import kotliquery.Session
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.HelseDao.Companion.single
import java.util.UUID

class MetrikkDao(private val session: Session) {
    /**
     Denne funksjonen antar at den kun kalles for en **ferdigbehandlet** kommandokjede for **godkjenningsbehov**.
     */
    fun finnUtfallForGodkjenningsbehov(contextId: UUID): GodkjenningsbehovUtfall {
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
            from oppgave
            where command_context_id = :contextId
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

enum class GodkjenningsbehovUtfall {
    AutomatiskAvvist,
    AutomatiskGodkjent,
    ManuellOppgave,
    Avbrutt,
}
