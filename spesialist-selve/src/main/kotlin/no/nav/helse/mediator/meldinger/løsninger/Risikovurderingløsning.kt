package no.nav.helse.mediator.meldinger.løsninger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.RisikovurderingRepository
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

internal class Risikovurderingløsning(
    private val vedtaksperiodeId: UUID,
    private val opprettet: LocalDateTime,
    val kanGodkjennesAutomatisk: Boolean,
    private val løsning: JsonNode,
) {
    private companion object {
        private val logg = LoggerFactory.getLogger(Risikovurderingløsning::class.java)
    }

    internal fun lagre(risikovurderingRepository: RisikovurderingRepository) {
        logg.info("Mottok risikovurdering for vedtaksperiode $vedtaksperiodeId")
        risikovurderingRepository.lagre(
            vedtaksperiodeId = vedtaksperiodeId,
            opprettet = opprettet,
            kanGodkjennesAutomatisk = kanGodkjennesAutomatisk,
            data = løsning,
        )
    }

    internal fun gjelderVedtaksperiode(vedtaksperiodeId: UUID) = this.vedtaksperiodeId == vedtaksperiodeId
}
