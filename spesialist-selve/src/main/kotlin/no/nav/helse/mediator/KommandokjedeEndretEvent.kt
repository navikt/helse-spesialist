package no.nav.helse.mediator

import java.util.UUID

sealed class KommandokjedeEndretEvent {
    abstract val commandContextId: UUID
    abstract val hendelseId: UUID

    abstract val eventName: String

    fun detaljer(): Map<String, Any> =
        mapOf(
            "commandContextId" to commandContextId,
            "meldingId" to hendelseId,
        ) + ekstraDetaljer()

    protected open fun ekstraDetaljer(): Map<String, Any> = emptyMap()

    class Ferdig(
        private val kommandonavn: String,
        override val commandContextId: UUID,
        override val hendelseId: UUID,
    ) : KommandokjedeEndretEvent() {
        override val eventName: String = "kommandokjede_ferdigstilt"

        override fun ekstraDetaljer(): Map<String, Any> {
            return mapOf("command" to kommandonavn)
        }
    }

    class Suspendert(
        private val kommandonavn: String,
        override val commandContextId: UUID,
        override val hendelseId: UUID,
    ) : KommandokjedeEndretEvent() {
        override val eventName: String = "kommandokjede_suspendert"

        override fun ekstraDetaljer(): Map<String, Any> {
            return mapOf("command" to kommandonavn)
        }
    }

    class Avbrutt(
        override val commandContextId: UUID,
        override val hendelseId: UUID,
    ) : KommandokjedeEndretEvent() {
        override val eventName: String = "kommandokjede_avbrutt"
    }
}
