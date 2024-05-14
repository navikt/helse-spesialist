package no.nav.helse.mediator.meldinger.løsninger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.varsel.Varselkode
import no.nav.helse.modell.varsel.Varselkode.SB_RV_2
import no.nav.helse.modell.varsel.Varselkode.SB_RV_3
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
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

    internal fun lagre(risikovurderingDao: RisikovurderingDao) {
        logg.info("Mottok risikovurdering for vedtaksperiode $vedtaksperiodeId")
        risikovurderingDao.lagre(
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

    internal class V2River(
        private val meldingMediator: MeldingMediator,
    ) : SpesialistRiver {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

        override fun validations() =
            River.PacketValidation {
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

        override fun onPacket(
            packet: JsonMessage,
            context: MessageContext,
        ) {
            sikkerLogg.info("Mottok melding RisikovurderingMessage:\n{}", packet.toJson())
            val opprettet = packet["@opprettet"].asLocalDateTime()
            val vedtaksperiodeId = UUID.fromString(packet["Risikovurdering.vedtaksperiodeId"].asText())
            val contextId = UUID.fromString(packet["contextId"].asText())
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())

            val løsning = packet["@løsning.Risikovurdering"]
            val kanGodkjennesAutomatisk = løsning["kanGodkjennesAutomatisk"].asBoolean()

            val risikovurdering =
                Risikovurderingløsning(
                    vedtaksperiodeId = vedtaksperiodeId,
                    opprettet = opprettet,
                    kanGodkjennesAutomatisk = kanGodkjennesAutomatisk,
                    løsning = løsning,
                )

            meldingMediator.løsning(
                hendelseId = hendelseId,
                contextId = contextId,
                behovId = UUID.fromString(packet["@id"].asText()),
                løsning = risikovurdering,
                context = context,
            )
        }
    }
}
