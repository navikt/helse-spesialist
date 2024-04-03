package no.nav.helse.mediator.meldinger

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeEndret
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

internal class VedtaksperiodeEndretRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: MeldingMediator,
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

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerlogg.error("Forstod ikke vedtaksperiode_endret:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        logg.info(
            "Mottok vedtaksperiode endret {}, {}, {}",
            keyValue("vedtaksperiodeId", UUID.fromString(packet["vedtaksperiodeId"].asText())),
            keyValue("eventId", UUID.fromString(packet["@id"].asText())),
            keyValue("forårsaketAvId", UUID.fromString(packet["@forårsaket_av.id"].asText())),
        )
        mediator.mottaMelding(VedtaksperiodeEndret(packet), context)
    }
}
