package no.nav.helse.mediator

import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.HelseDao

class MetrikkDao(dataSource: DataSource) : HelseDao(dataSource) {
    fun finnUtfallForGodkjenningsbehov(contextId: UUID): GodkjenningsbehovUtfall {
        val bleAutomatiskGodkjent = asSQL(
            """
            select distinct automatisert from automatisering
            join command_context cc on automatisering.hendelse_ref = cc.hendelse_id
            where context_id =  :contextId
            """, mapOf("contextId" to contextId)
        ).single { row -> row.boolean("automatisert") } ?: false

        if (bleAutomatiskGodkjent) return GodkjenningsbehovUtfall.AutomatiskGodkjent

        val gikkTilManuell = asSQL(
            """
            select distinct 1
            from oppgave
            where command_context_id = :contextId
            """, mapOf("contextId" to contextId)
        ).single { true } ?: false

        return if (gikkTilManuell) GodkjenningsbehovUtfall.ManuellOppgave
        else GodkjenningsbehovUtfall.AutomatiskAvvist
    }
}

enum class GodkjenningsbehovUtfall {
    AutomatiskAvvist, AutomatiskGodkjent, ManuellOppgave;
}
