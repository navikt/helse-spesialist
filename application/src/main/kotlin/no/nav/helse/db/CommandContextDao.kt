package no.nav.helse.db

import no.nav.helse.modell.kommando.CommandContext
import java.util.UUID

interface CommandContextDao {
    fun nyContext(meldingId: UUID): CommandContext

    fun opprett(
        hendelseId: UUID,
        contextId: UUID,
    )

    fun ferdig(
        hendelseId: UUID,
        contextId: UUID,
    )

    fun suspendert(
        hendelseId: UUID,
        contextId: UUID,
        hash: UUID,
        sti: List<Int>,
    )

    fun feil(
        hendelseId: UUID,
        contextId: UUID,
    )

    fun tidsbrukForContext(contextId: UUID): Int

    fun avbryt(
        vedtaksperiodeId: UUID,
        contextId: UUID,
    ): List<Pair<UUID, UUID>>

    fun finnSuspendert(contextId: UUID): CommandContext?

    fun finnSuspendertEllerFeil(contextId: UUID): CommandContext?
}
