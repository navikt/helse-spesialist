package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeForkastet
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

internal class VedtaksperiodeForkastetRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    private companion object {
        private fun uuid(jsonNode: JsonNode): UUID = UUID.fromString(jsonNode.asText())
    }

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "vedtaksperiode_forkastet")
            it.require("@id", Companion::uuid)
            it.require("vedtaksperiodeId", Companion::uuid)
            it.requireKey("fødselsnummer")
        }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerLogg.error("Forstod ikke vedtaksperiode_forkastet:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        mediator.mottaMelding(VedtaksperiodeForkastet(packet), context)
    }
}
