package no.nav.helse.spesialist.application

import no.nav.helse.db.CommandContextDao
import no.nav.helse.modell.kommando.CommandContext
import java.time.Duration
import java.time.Instant
import java.util.UUID

class InMemoryCommandContextDao : CommandContextDao {
    private val data = mutableListOf<Data>()

    private data class Data(
        val id: Int,
        val contextId: UUID,
        val hendelseId: UUID,
        val opprettet: Instant,
        val tilstand: CommandContextTilstand,
        val sti: List<Int>,
        val hash: UUID?,
    ) {
        enum class CommandContextTilstand { NY, FERDIG, SUSPENDERT, FEIL }
    }

    override fun nyContext(meldingId: UUID): CommandContext =
        UUID.randomUUID().let { contextId ->
            CommandContext(contextId).apply {
                opprett(meldingId, contextId)
            }
        }

    override fun opprett(hendelseId: UUID, contextId: UUID) {
        lagre(hendelseId, contextId, Data.CommandContextTilstand.NY, null)
    }

    override fun ferdig(hendelseId: UUID, contextId: UUID) {
        lagre(hendelseId, contextId, Data.CommandContextTilstand.FERDIG, null)
    }

    override fun feil(hendelseId: UUID, contextId: UUID) {
        lagre(hendelseId, contextId, Data.CommandContextTilstand.FEIL, null)
    }

    override fun suspendert(
        hendelseId: UUID,
        contextId: UUID,
        hash: UUID,
        sti: List<Int>,
    ) {
        lagre(hendelseId, contextId, Data.CommandContextTilstand.SUSPENDERT, hash, sti)
    }

    override fun avbryt(vedtaksperiodeId: UUID, contextId: UUID) = TODO("Ikke implementert for tester")

    private fun lagre(
        hendelseId: UUID,
        contextId: UUID,
        tilstand: Data.CommandContextTilstand,
        hash: UUID?,
        sti: List<Int> = emptyList(),
    ) {
        data.add(
            Data(
                id = (data.maxOfOrNull { it.id } ?: 0) + 1,
                contextId = contextId,
                hendelseId = hendelseId,
                opprettet = Instant.now(),
                tilstand = tilstand,
                sti = sti,
                hash = hash
            )
        )
    }

    override fun tidsbrukForContext(contextId: UUID) =
        data.filter { it.contextId == contextId }.let {
            Duration.between(it.minOf(Data::opprettet), it.maxOf(Data::opprettet)).toMillis().toInt()
        }

    override fun finnSuspendert(contextId: UUID) =
        data.filter { it.contextId == contextId }.maxByOrNull { it.id }
            ?.takeIf { it.tilstand == Data.CommandContextTilstand.SUSPENDERT }
            ?.let { CommandContext(id = contextId, sti = it.sti, hash = it.hash) }

    override fun finnSuspendertEllerFeil(contextId: UUID) =
        data.filter { it.contextId == contextId }.maxByOrNull { it.id }
            ?.takeIf { it.tilstand == Data.CommandContextTilstand.SUSPENDERT || it.tilstand == Data.CommandContextTilstand.FEIL }
            ?.let { CommandContext(id = contextId, sti = it.sti, hash = it.hash) }
}
