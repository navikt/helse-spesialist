package no.nav.helse.mediator.meldinger

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeReberegnet
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class VedtaksperiodeReberegnetRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val log = LoggerFactory.getLogger(this::class.java)

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
        log.info("Håndterer reberegning av vedtaksperiode: ${packet["vedtaksperiodeId"].asText()}")
        mediator.mottaMelding(VedtaksperiodeReberegnet(packet), context)
    }
}
