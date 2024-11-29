package no.nav.helse.kafka

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeReberegnet
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River

internal class VedtaksperiodeReberegnetRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "vedtaksperiode_endret")
            it.demand("forrigeTilstand") { node -> check(node.asText().startsWith("AVVENTER_GODKJENNING")) }
            it.rejectValues("gjeldendeTilstand", listOf("AVSLUTTET", "TIL_UTBETALING", "TIL_INFOTRYGD"))
            it.requireKey(
                "@id",
                "fødselsnummer",
                "vedtaksperiodeId",
            )
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        mediator.mottaMelding(VedtaksperiodeReberegnet(packet), context)
    }
}
