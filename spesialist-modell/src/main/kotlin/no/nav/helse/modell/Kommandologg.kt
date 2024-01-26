package no.nav.helse.modell

import java.time.LocalDateTime
import java.util.UUID

class Kommandologg private constructor(private val forelder: Kommandologg? = null) {
    private val kontekster = mutableSetOf<KommandologgKontekst>()
    private val logginnslag = mutableListOf<Logginnslag>()

    fun kontekst(kommandonavn: String) {
        kontekster.add(KommandologgKontekst(kommandonavn))
    }

    fun nyttInnslag(melding: String) {
        val innslag = Logginnslag(melding, kontekster)
        logginnslag.addLast(innslag)
        forelder?.logginnslag?.addLast(innslag)
    }

    fun barn() = Kommandologg(forelder = this).also {
        it.kontekster.addAll(this.kontekster)
    }

    internal fun alleInnslag() = logginnslag.toList()

    companion object {
        fun nyLogg() = Kommandologg()
    }
}

internal data class KommandologgKontekst(internal val navn: String)

internal class Logginnslag(
    internal val melding: String,
    internal val kontekster: Set<KommandologgKontekst>
) {
    internal val id: UUID = UUID.randomUUID()
    internal val opprettet: LocalDateTime = LocalDateTime.now()
}