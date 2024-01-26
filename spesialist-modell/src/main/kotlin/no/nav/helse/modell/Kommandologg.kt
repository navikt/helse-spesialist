package no.nav.helse.modell

import java.time.LocalDateTime
import java.util.UUID

class Kommandologg private constructor(private val forelder: Kommandologg? = null) {
    private val kontekster = mutableSetOf<KommandologgKontekst>()
    private val logginnslag = mutableListOf<Logginnslag>()

    fun accept(kommandologgVisitor: KommandologgVisitor) {
        logginnslag.forEach {
            it.accept(kommandologgVisitor)
        }
    }

    fun kontekst(kommandonavn: String) {
        kontekster.add(KommandologgKontekst(kommandonavn))
    }

    fun nyttInnslag(melding: String) {
        val innslag = Logginnslag(melding, kontekster)
        nyttInnslag(innslag)
    }

    private fun nyttInnslag(innslag: Logginnslag) {
        this.logginnslag.addLast(innslag)
        forelder?.nyttInnslag(innslag)
    }

    fun barn() = Kommandologg(forelder = this).also {
        it.kontekster.addAll(this.kontekster)
    }

    companion object {
        fun nyLogg() = Kommandologg()
    }
}

private data class KommandologgKontekst(val navn: String)

private class Logginnslag(
    private val melding: String,
    private val kontekster: Set<KommandologgKontekst>
) {
    val id: UUID = UUID.randomUUID()
    val opprettet: LocalDateTime = LocalDateTime.now()

    fun accept(kommandologgVisitor: KommandologgVisitor) {
        kommandologgVisitor.visitInnslag(id, opprettet, melding, kontekster.map { it.navn })
    }
}