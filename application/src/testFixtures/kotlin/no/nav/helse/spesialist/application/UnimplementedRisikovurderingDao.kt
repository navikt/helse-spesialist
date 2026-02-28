package no.nav.helse.spesialist.application

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.RisikovurderingDao
import no.nav.helse.modell.risiko.Risikovurdering
import java.time.LocalDateTime
import java.util.UUID

class UnimplementedRisikovurderingDao : RisikovurderingDao {
    override fun hentRisikovurdering(vedtaksperiodeId: UUID): Risikovurdering? {
        TODO("Not yet implemented")
    }

    override fun måTilManuell(vedtaksperiodeId: UUID): Boolean {
        TODO("Not yet implemented")
    }

    override fun lagre(
        vedtaksperiodeId: UUID,
        kanGodkjennesAutomatisk: Boolean,
        data: JsonNode,
        opprettet: LocalDateTime
    ) {
        TODO("Not yet implemented")
    }
}
