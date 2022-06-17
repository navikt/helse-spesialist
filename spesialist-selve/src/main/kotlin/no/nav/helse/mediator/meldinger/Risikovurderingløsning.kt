package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

internal class Risikovurderingløsning(
    private val vedtaksperiodeId: UUID,
    private val opprettet: LocalDateTime,
    private val kanGodkjennesAutomatisk: Boolean,
    private val løsning: JsonNode,
) {
    private companion object {
        private val logg = LoggerFactory.getLogger(Risikovurderingløsning::class.java)
    }

    internal fun lagre(risikovurderingDao: RisikovurderingDao) {
        logg.info("Mottok risikovurdering for vedtaksperiode $vedtaksperiodeId")
        risikovurderingDao.lagre(
            vedtaksperiodeId = vedtaksperiodeId,
            opprettet = opprettet,
            kanGodkjennesAutomatisk = kanGodkjennesAutomatisk,
            kreverSupersaksbehandler = løsning["funn"].any { it["kreverSupersaksbehandler"].asBoolean() },
            data = løsning
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

    internal fun harFaresignalerFunn() =
        !kanGodkjennesAutomatisk && løsning["funn"].any { !it["kategori"].toList().map { it.asText() }.contains("8-4") }

    internal class V2River(
        rapidsConnection: RapidsConnection,
        private val hendelseMediator: HendelseMediator
    ) : River.PacketListener {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.requireKey("@id")
                    it.demandValue("@event_name", "behov")
                    it.demandValue("@final", true)
                    it.demandAll("@behov", listOf("Risikovurdering"))
                    it.require("@opprettet") { message -> message.asLocalDateTime() }
                    it.require("Risikovurdering.vedtaksperiodeId") { message -> UUID.fromString(message.asText()) }
                    it.demandKey("contextId")
                    it.demandKey("hendelseId")
                    it.requireKey("@løsning.Risikovurdering")
                    it.requireKey(
                        "@løsning.Risikovurdering.kanGodkjennesAutomatisk",
                        "@løsning.Risikovurdering.funn",
                        "@løsning.Risikovurdering.kontrollertOk",
                    )
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            sikkerLogg.info("Mottok melding RisikovurderingMessage:\n{}", packet.toJson())
            val opprettet = packet["@opprettet"].asLocalDateTime()
            val vedtaksperiodeId = UUID.fromString(packet["Risikovurdering.vedtaksperiodeId"].asText())
            val contextId = UUID.fromString(packet["contextId"].asText())
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())

            val løsning = packet["@løsning.Risikovurdering"]
            val kanGodkjennesAutomatisk = løsning["kanGodkjennesAutomatisk"].asBoolean()

            val risikovurdering = Risikovurderingløsning(
                vedtaksperiodeId = vedtaksperiodeId,
                opprettet = opprettet,
                kanGodkjennesAutomatisk = kanGodkjennesAutomatisk,
                løsning = løsning,
            )

            hendelseMediator.løsning(
                hendelseId = hendelseId,
                contextId = contextId,
                behovId = UUID.fromString(packet["@id"].asText()),
                løsning = risikovurdering,
                context = context
            )
        }
    }
}
