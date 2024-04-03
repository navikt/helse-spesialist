package no.nav.helse.mediator.meldinger

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.overstyring.OverstyringIgangsatt
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

internal class OverstyringIgangsattRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: MeldingMediator,
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

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerLogg.error("Forstod ikke overstyring_igangsatt:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        if (packet["berørtePerioder"].isEmpty) {
            sikkerLogg.info(
                "Overstyring med {} har ingen berørte perioder.",
                keyValue("kilde", UUID.fromString(packet["kilde"].asText())),
            )
            return
        }

        log.info(
            "Mottok overstyring igangsatt {}, {}, {}",
            keyValue(
                "berørteVedtaksperiodeIder",
                packet["berørtePerioder"]
                    .map { UUID.fromString(it["vedtaksperiodeId"].asText()) },
            ),
            keyValue("eventId", UUID.fromString(packet["@id"].asText())),
            keyValue("kilde", UUID.fromString(packet["kilde"].asText())),
        )
        mediator.mottaMelding(OverstyringIgangsatt(packet), context)
    }
}
