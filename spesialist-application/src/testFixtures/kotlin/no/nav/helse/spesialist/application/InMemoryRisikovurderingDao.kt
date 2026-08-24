package no.nav.helse.spesialist.application

import no.nav.helse.db.RisikovurderingDao
import no.nav.helse.modell.automatisering.sjekker.Risikovurdering
import tools.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID

class InMemoryRisikovurderingDao : RisikovurderingDao {
    private data class LagretRisikovurdering(
        val vedtaksperiodeId: UUID,
        val kanGodkjennesAutomatisk: Boolean,
        val data: JsonNode,
        val opprettet: LocalDateTime,
    )

    private val risikovurderinger = mutableListOf<LagretRisikovurdering>()

    override fun hentRisikovurdering(vedtaksperiodeId: UUID): Risikovurdering? = risikovurderinger.lastOrNull { it.vedtaksperiodeId == vedtaksperiodeId }?.let { Risikovurdering.restore(it.kanGodkjennesAutomatisk) }

    override fun måTilManuell(vedtaksperiodeId: UUID): Boolean = risikovurderinger.lastOrNull { it.vedtaksperiodeId == vedtaksperiodeId }?.kanGodkjennesAutomatisk?.not() ?: false

    fun antallLagret(vedtaksperiodeId: UUID): Int = risikovurderinger.count { it.vedtaksperiodeId == vedtaksperiodeId }

    override fun lagre(
        vedtaksperiodeId: UUID,
        kanGodkjennesAutomatisk: Boolean,
        data: JsonNode,
        opprettet: LocalDateTime,
    ) {
        risikovurderinger.add(LagretRisikovurdering(vedtaksperiodeId, kanGodkjennesAutomatisk, data, opprettet))
    }
}
