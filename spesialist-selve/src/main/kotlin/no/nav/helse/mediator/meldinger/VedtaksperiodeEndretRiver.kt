package no.nav.helse.mediator.meldinger

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class VedtaksperiodeEndretRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: HendelseMediator
) : River.PacketListener {

    private val logg = LoggerFactory.getLogger(this::class.java)
    private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "vedtaksperiode_endret")
                it.rejectValue("forrigeTilstand", "START")
                it.requireKey("vedtaksperiodeId")
                it.requireKey("fødselsnummer")
                it.requireKey("@id")
                it.requireKey("@forårsaket_av", "@forårsaket_av.id")
                it.interestedIn("forrigeTilstand")
                it.interestedIn("gjeldendeTilstand")
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerlogg.error("Forstod ikke vedtaksperiode_endret:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
        val id = UUID.fromString(packet["@id"].asText())
        val forårsaketAvId = UUID.fromString(packet["@forårsaket_av.id"].asText())
        logg.info(
            "Mottok vedtaksperiode endret {}, {}, {}",
            keyValue("vedtaksperiodeId", vedtaksperiodeId),
            keyValue("eventId", id),
            keyValue("forårsaketAvId", forårsaketAvId),
        )
        mediator.vedtaksperiodeEndret(
            message = packet,
            id = id,
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = packet["fødselsnummer"].asText(),
            forårsaketAvId = forårsaketAvId,
            forrigeTilstand = packet["forrigeTilstand"].asText(),
            gjeldendeTilstand = packet["gjeldendeTilstand"].asText(),
            context = context
        )
    }
}