package no.nav.helse.mediator

import no.nav.helse.HelseDao
import java.util.UUID
import javax.sql.DataSource

class MetrikkDao(dataSource: DataSource) : HelseDao(dataSource) {
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
                mapOf("contextId" to contextId),
            ).single { row -> row.int(1) }!!

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
                mapOf("contextId" to contextId),
            ).single { row -> row.boolean("automatisert") } ?: false

        if (bleAutomatiskGodkjent) return GodkjenningsbehovUtfall.AutomatiskGodkjent

        val gikkTilManuell =
            asSQL(
                """
            select distinct 1
            from oppgave
            where command_context_id = :contextId
            """,
                mapOf("contextId" to contextId),
            ).single { true } ?: false

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
