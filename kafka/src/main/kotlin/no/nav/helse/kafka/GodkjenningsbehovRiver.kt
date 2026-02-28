package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov

class GodkjenningsbehovRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireAll("@behov", listOf("Godkjenning"))
            it.forbid("@løsning")
            it.forbidValue("yrkesaktivitetstype", "JORDBRUKER")
        }

    override fun validations() = River.PacketValidation { }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        mediator.mottaMelding(
            melding = Godkjenningsbehov.fraJson(packet.toJson()),
            kontekstbasertPubliserer = MessageContextMeldingPubliserer(context),
        )
    }
}
