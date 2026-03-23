package no.nav.helse.db

import java.util.UUID

interface MetrikkDao {
    /**
     Denne funksjonen antar at den kun kalles for en **ferdigbehandlet** kommandokjede for **godkjenningsbehov**.
     */
    fun finnUtfallForGodkjenningsbehov(contextId: UUID): GodkjenningsbehovUtfall
}

enum class GodkjenningsbehovUtfall {
    AutomatiskAvvist,
    AutomatiskGodkjent,
    ManuellOppgave,
    Avbrutt,
}
