package no.nav.helse.mediator.meldinger

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeEndret
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

internal class VedtaksperiodeEndretRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val logg = LoggerFactory.getLogger(this::class.java)
    private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "vedtaksperiode_endret")
            it.demandValue("gjeldendeTilstand", "AVSLUTTET")
            it.requireKey("vedtaksperiodeId")
            it.requireKey("fødselsnummer")
            it.requireKey("@id")
            it.requireKey("@forårsaket_av", "@forårsaket_av.id")
            it.interestedIn("forrigeTilstand")
            it.interestedIn("gjeldendeTilstand")
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
        if (packet["vedtaksperiodeId"].asText() in
            setOf(
                "5bc7f788-0b34-410e-9d7a-21932c1c5be7",
                "1d04a606-893f-43ca-a102-29b3a48f5f74",
                "d244cffb-e6dd-4c06-bb60-4713c44329b3",
            )
        ) {
            logg.info("ignorerer flokemelding")
            return
        }
        logg.info(
            "Mottok vedtaksperiode endret {}, {}, {}",
            keyValue("vedtaksperiodeId", UUID.fromString(packet["vedtaksperiodeId"].asText())),
            keyValue("eventId", UUID.fromString(packet["@id"].asText())),
            keyValue("forårsaketAvId", UUID.fromString(packet["@forårsaket_av.id"].asText())),
        )
        mediator.mottaMelding(VedtaksperiodeEndret(packet), context)
    }
}
