package no.nav.helse.db

import no.nav.helse.modell.automatisering.sjekker.Risikovurdering
import no.nav.helse.spesialist.api.risikovurdering.RisikovurderingApiDto
import tools.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID

interface RisikovurderingDao {
    fun hentRisikovurdering(vedtaksperiodeId: UUID): Risikovurdering?

    fun hentRisikovurderingsdata(vedtaksperiodeId: UUID): RisikovurderingApiDto?

    fun måTilManuell(vedtaksperiodeId: UUID): Boolean

    fun lagre(
        vedtaksperiodeId: UUID,
        kanGodkjennesAutomatisk: Boolean,
        data: JsonNode,
        opprettet: LocalDateTime,
    )
}
