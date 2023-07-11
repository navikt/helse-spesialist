package no.nav.helse.mediator.meldinger

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class OverstyringIgangsattRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: HendelseMediator
) : River.PacketListener {

    private val log = LoggerFactory.getLogger(this::class.java)
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "overstyring_igangsatt")
                it.requireKey("kilde")
                it.requireArray("berørtePerioder") {
                    requireKey("vedtaksperiodeId")
                }
                it.requireKey("@id")
                it.requireKey("fødselsnummer")
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerLogg.error("Forstod ikke overstyring_igangsatt:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val id = UUID.fromString(packet["@id"].asText())
        val kilde = UUID.fromString(packet["kilde"].asText())

        val berørtePerioderJson = packet["berørtePerioder"]
        if (berørtePerioderJson.isEmpty) {
            sikkerLogg.info(
                "Overstyring med {} har ingen berørte perioder.",
                StructuredArguments.keyValue("kilde", kilde)
            )
            return
        }
        val berørteVedtaksperiodeIder = berørtePerioderJson
            .map { UUID.fromString(it["vedtaksperiodeId"].asText()) }

        log.info(
            "Mottok overstyring igangsatt {}, {}, {}",
            StructuredArguments.keyValue("berørteVedtaksperiodeIder", berørteVedtaksperiodeIder),
            StructuredArguments.keyValue("eventId", id),
            StructuredArguments.keyValue("kilde", kilde),
        )
        mediator.overstyringIgangsatt(
            packet,
            id,
            packet["fødselsnummer"].asText(),
            kilde,
            berørteVedtaksperiodeIder,
            context
        )
    }
}