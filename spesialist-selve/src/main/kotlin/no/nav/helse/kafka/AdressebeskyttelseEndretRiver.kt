package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.meldinger.AdressebeskyttelseEndret

internal class AdressebeskyttelseEndretRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "adressebeskyttelse_endret")
            it.requireKey("@id", "fødselsnummer")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        mediator.mottaMelding(melding = AdressebeskyttelseEndret(packet), messageContext = context)
    }
}
