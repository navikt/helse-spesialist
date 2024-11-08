package no.nav.helse.mediator.meldinger.løsninger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.db.RisikovurderingRepository
import no.nav.helse.modell.varsel.Varselkode
import no.nav.helse.modell.varsel.Varselkode.SB_RV_2
import no.nav.helse.modell.varsel.Varselkode.SB_RV_3
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

internal class Risikovurderingløsning(
    private val vedtaksperiodeId: UUID,
    private val opprettet: LocalDateTime,
    private val kanGodkjennesAutomatisk: Boolean,
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
            kreverSupersaksbehandler = løsning["funn"].any { it["kreverSupersaksbehandler"].asBoolean() },
            data = løsning,
        )
    }

    internal fun gjelderVedtaksperiode(vedtaksperiodeId: UUID) = this.vedtaksperiodeId == vedtaksperiodeId

    internal fun harArbeidsuførhetFunn() =
        !kanGodkjennesAutomatisk && løsning["funn"].any { it["kategori"].toList().map { it.asText() }.contains("8-4") }

    internal fun arbeidsuførhetsmelding(): String =
        "Arbeidsuførhet, aktivitetsplikt og/eller medvirkning må vurderes." +
            løsning["funn"]
                .filter { funn -> funn["kategori"].toList().map(JsonNode::asText).contains("8-4") }
                .map { it["beskrivelse"].asText() }
                .takeIf { it.isNotEmpty() }
                ?.let { "\n" + it.joinToString(" ") }

    internal fun varselkode(): Varselkode {
        val riskbeskrivelser =
            løsning["funn"]
                .filter { funn -> funn["kategori"].toList().map(JsonNode::asText).contains("8-4") }
                .map { it["beskrivelse"].asText() }
        val feilmelding =
            "Klarte ikke gjøre automatisk 8-4-vurdering p.g.a. teknisk feil. Kan godkjennes hvis alt ser greit ut."
        if (riskbeskrivelser.contains(feilmelding)) return SB_RV_3
        return SB_RV_2
    }

    internal fun harFaresignalerFunn() =
        !kanGodkjennesAutomatisk && løsning["funn"].any { !it["kategori"].toList().map { it.asText() }.contains("8-4") }
}
