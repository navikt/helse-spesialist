package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.kommando.TilbakedateringBehandlet

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
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        mediator.mottaMelding(TilbakedateringBehandlet(packet), context)
    }
}
