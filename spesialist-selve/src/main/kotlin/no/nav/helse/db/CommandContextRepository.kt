package no.nav.helse.db

import java.util.UUID

internal interface CommandContextRepository {
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
}
