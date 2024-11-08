package no.nav.helse.kafka

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.kommando.TilbakedateringBehandlet
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River

// I skrivende stund er det kun meldinger der tilbakedateringen er godkjent som
// kommer til Spesialist, dvs. sendes på rapiden. Andre meldinger filtreres ut i sparkel-appen
internal class TilbakedateringBehandletRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "tilbakedatering_behandlet")
            it.requireKey("@opprettet")
            it.requireKey("@id")
            it.requireKey("fødselsnummer")
            it.requireKey("sykmeldingId")
            it.requireArray("perioder") {
                requireKey("fom", "tom")
            }
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        mediator.mottaMelding(TilbakedateringBehandlet(packet), context)
    }
}
