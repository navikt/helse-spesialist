package no.nav.helse.mediator.meldinger

import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class VedtaksperiodeReberegnetRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: HendelseMediator
) : River.PacketListener {
    private val log = LoggerFactory.getLogger(this::class.java)

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "vedtaksperiode_endret")
                it.demand("forrigeTilstand") { node -> check(node.asText().startsWith("AVVENTER_GODKJENNING")) }
                it.rejectValues("gjeldendeTilstand", listOf("AVSLUTTET", "TIL_UTBETALING", "TIL_INFOTRYGD"))
                it.requireKey(
                    "@id", "fødselsnummer", "vedtaksperiodeId"
                )
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        log.info("Avbryter saksbehandling på vedtaksperiode: ${packet["vedtaksperiodeId"].asText()}")
        mediator.avbrytSaksbehandling(packet, context)
    }
}