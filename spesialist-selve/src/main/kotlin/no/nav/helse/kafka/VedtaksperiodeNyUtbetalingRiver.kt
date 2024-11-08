package no.nav.helse.kafka

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeNyUtbetaling
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River

internal class VedtaksperiodeNyUtbetalingRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "vedtaksperiode_ny_utbetaling")
            it.requireKey("@id", "fødselsnummer", "vedtaksperiodeId", "utbetalingId")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        mediator.mottaMelding(VedtaksperiodeNyUtbetaling(packet), context)
    }
}
