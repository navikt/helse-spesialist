package no.nav.helse.kafka

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.meldinger.AdressebeskyttelseEndret
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River

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
    ) {
        mediator.mottaMelding(melding = AdressebeskyttelseEndret(packet), messageContext = context)
    }
}
