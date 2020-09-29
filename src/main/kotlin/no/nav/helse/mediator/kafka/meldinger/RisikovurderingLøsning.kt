package no.nav.helse.mediator.kafka.meldinger

import no.nav.helse.mediator.kafka.FeatureToggle
import no.nav.helse.mediator.kafka.HendelseMediator
import no.nav.helse.modell.risiko.RisikovurderingDao
import no.nav.helse.modell.risiko.RisikovurderingDto
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

internal class RisikovurderingLøsning(
    private val hendelseId: UUID,
    private val vedtaksperiodeId: UUID,
    private val opprettet: LocalDateTime,
    private val samletScore: Double,
    private val begrunnelser: List<String>,
    private val ufullstendig: Boolean,
    private val faresignaler: List<String>,
    private val arbeidsuførhetvurdering: List<String>
) {
    internal fun lagre(risikovurderingDao: RisikovurderingDao) {
        risikovurderingDao.persisterRisikovurdering(
            RisikovurderingDto(
                vedtaksperiodeId = vedtaksperiodeId,
                opprettet = opprettet,
                samletScore = samletScore,
                faresignaler = faresignaler,
                arbeidsuførhetvurdering = arbeidsuførhetvurdering,
                ufullstendig = ufullstendig
            )
        )
    }

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
                        "@løsning.Risikovurdering.samletScore",
                        "@løsning.Risikovurdering.begrunnelser",
                        "@løsning.Risikovurdering.ufullstendig",
                        "@løsning.Risikovurdering.begrunnelserSomAleneKreverManuellBehandling"
                    )
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            sikkerLogg.info("Mottok melding RisikovurderingMessage: ", packet.toJson())
            val opprettet = packet["@opprettet"].asLocalDateTime()
            val vedtaksperiodeId = UUID.fromString(packet["Risikovurdering.vedtaksperiodeId"].asText())
            val contextId = UUID.fromString(packet["contextId"].asText())
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())

            val løsning = packet["@løsning.Risikovurdering"]
            val samletScore = løsning["samletScore"].asDouble()
            val ufullstendig = løsning["ufullstendig"].asBoolean()
            val faresignaler = løsning["begrunnelser"].map { it.asText() }
            val arbeidsuførhetvurdering = løsning["begrunnelserSomAleneKreverManuellBehandling"].map { it.asText() }

            val risikovurdering = RisikovurderingLøsning(
                hendelseId = hendelseId,
                vedtaksperiodeId = vedtaksperiodeId,
                opprettet = opprettet,
                samletScore = samletScore,
                begrunnelser = faresignaler,
                ufullstendig = ufullstendig,
                faresignaler = faresignaler,
                arbeidsuførhetvurdering = arbeidsuførhetvurdering
            )

            if(FeatureToggle.nyGodkjenningRiver) {
                hendelseMediator.løsning(
                    hendelseId = hendelseId,
                    contextId = contextId,
                    løsning = risikovurdering,
                    context = context
                )
            } else {
                hendelseMediator.håndter(hendelseId, risikovurdering)
            }
        }
    }

    internal class V1River(
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
                    it.require("vedtaksperiodeId") { message -> UUID.fromString(message.asText()) }
                    it.demandKey("spleisBehovId")
                    it.requireKey("@løsning.Risikovurdering")
                    it.requireKey(
                        "@løsning.Risikovurdering.samletScore",
                        "@løsning.Risikovurdering.begrunnelser",
                        "@løsning.Risikovurdering.ufullstendig",
                        "@løsning.Risikovurdering.begrunnelserSomAleneKreverManuellBehandling"
                    )
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            sikkerLogg.info("Mottok melding RisikovurderingMessage: ", packet.toJson())
            val opprettet = packet["@opprettet"].asLocalDateTime()
            val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
            val contextId = UUID.fromString(packet["contextId"].asText())
            val hendelseId = UUID.fromString(packet["spleisBehovId"].asText())

            val løsning = packet["@løsning.Risikovurdering"]
            val samletScore = løsning["samletScore"].asDouble()
            val ufullstendig = løsning["ufullstendig"].asBoolean()
            val faresignaler = løsning["begrunnelser"].map { it.asText() }
            val arbeidsuførhetvurdering = løsning["begrunnelserSomAleneKreverManuellBehandling"].map { it.asText() }

            val risikovurdering = RisikovurderingLøsning(
                hendelseId = hendelseId,
                vedtaksperiodeId = vedtaksperiodeId,
                opprettet = opprettet,
                samletScore = samletScore,
                begrunnelser = faresignaler,
                ufullstendig = ufullstendig,
                faresignaler = faresignaler,
                arbeidsuførhetvurdering = arbeidsuførhetvurdering
            )

            if(FeatureToggle.nyGodkjenningRiver) {
                hendelseMediator.løsning(
                    hendelseId = hendelseId,
                    contextId = contextId,
                    løsning = risikovurdering,
                    context = context
                )
            } else {
                hendelseMediator.håndter(hendelseId, risikovurdering)
            }
        }
    }
}
