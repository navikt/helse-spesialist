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
    val andreVedtaksperioder: List<AnnenVedtaksperiode> = emptyList(),
) {
    // Risikovurdering av en annen vedtaksperiode (med samme skjæringstidspunkt) som riskmodulen har gjort i samme slengen
    class AnnenVedtaksperiode(
        val vedtaksperiodeId: UUID,
        val kanGodkjennesAutomatisk: Boolean,
        val løsning: JsonNode,
    )

    internal fun lagre(risikovurderingDao: RisikovurderingDao) {
        logg.info("Mottok risikovurdering for vedtaksperiode $vedtaksperiodeId")
        risikovurderingDao.lagre(
            vedtaksperiodeId = vedtaksperiodeId,
            opprettet = opprettet,
            kanGodkjennesAutomatisk = kanGodkjennesAutomatisk,
            data = løsning,
        )
    }

    internal fun lagre(
        annenVedtaksperiode: AnnenVedtaksperiode,
        risikovurderingDao: RisikovurderingDao,
    ) {
        logg.info(
            "Lagrer risikovurdering for vedtaksperiode ${annenVedtaksperiode.vedtaksperiodeId}, mottatt sammen med risikovurdering av vedtaksperiode $vedtaksperiodeId",
        )
        risikovurderingDao.lagre(
            vedtaksperiodeId = annenVedtaksperiode.vedtaksperiodeId,
            opprettet = opprettet,
            kanGodkjennesAutomatisk = annenVedtaksperiode.kanGodkjennesAutomatisk,
            data = annenVedtaksperiode.løsning,
        )
    }

    internal fun gjelderVedtaksperiode(vedtaksperiodeId: UUID) = this.vedtaksperiodeId == vedtaksperiodeId
}
