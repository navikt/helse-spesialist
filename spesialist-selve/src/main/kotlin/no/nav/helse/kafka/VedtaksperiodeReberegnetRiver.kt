package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeReberegnet

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
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        mediator.mottaMelding(VedtaksperiodeReberegnet(packet), context)
    }
}
