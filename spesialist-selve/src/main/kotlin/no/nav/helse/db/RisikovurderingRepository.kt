package no.nav.helse.db

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.modell.risiko.Risikovurdering
import java.time.LocalDateTime
import java.util.UUID

interface RisikovurderingRepository {
    fun hentRisikovurdering(vedtaksperiodeId: UUID): Risikovurdering?

    fun kreverSupersaksbehandler(vedtaksperiodeId: UUID): Boolean

    fun lagre(
        vedtaksperiodeId: UUID,
        kanGodkjennesAutomatisk: Boolean,
        kreverSupersaksbehandler: Boolean,
        data: JsonNode,
        opprettet: LocalDateTime,
    )
}
