package no.nav.helse.modell

import java.time.LocalDateTime
import java.util.UUID

class Kommandologg {
    private val kontekster = mutableSetOf<KommandologgKontekst>()
    private val logginnslag = mutableListOf<Logginnslag>()

    fun kontekst(kommandonavn: String) {
        kontekster.add(KommandologgKontekst(kommandonavn))
    }

    fun nyttInnslag(melding: String) {
        logginnslag.addLast(Logginnslag(melding, kontekster))
    }

    internal fun alleInnslag() = logginnslag.toList()
}

internal data class KommandologgKontekst(internal val navn: String)

internal class Logginnslag(
    internal val melding: String,
    internal val kontekster: Set<KommandologgKontekst>
) {
    internal val id: UUID = UUID.randomUUID()
    internal val opprettet: LocalDateTime = LocalDateTime.now()
}