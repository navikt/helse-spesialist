package no.nav.helse.modell

import java.time.LocalDateTime
import java.util.UUID

interface KommandologgVisitor {
    fun visitInnslag(id: UUID, opprettet: LocalDateTime, melding: String, kontekster: List<String>) {}
}