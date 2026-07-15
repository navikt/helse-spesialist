package no.nav.helse.mediator.meldinger.løsninger

import no.nav.helse.db.RisikovurderingDao
import no.nav.helse.spesialist.application.logg.logg
import tools.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID

class Risikovurderingløsning(
    private val vedtaksperiodeId: UUID,
    private val opprettet: LocalDateTime,
    val kanGodkjennesAutomatisk: Boolean,
    private val løsning: JsonNode,
) {
    internal fun lagre(risikovurderingDao: RisikovurderingDao) {
        logg.info("Mottok risikovurdering for vedtaksperiode $vedtaksperiodeId")
        risikovurderingDao.lagre(
            vedtaksperiodeId = vedtaksperiodeId,
            opprettet = opprettet,
            kanGodkjennesAutomatisk = kanGodkjennesAutomatisk,
            data = løsning,
        )
    }

    internal fun gjelderVedtaksperiode(vedtaksperiodeId: UUID) = this.vedtaksperiodeId == vedtaksperiodeId
}
