package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.AdressebeskyttelseEndret

class AdressebeskyttelseEndretRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun preconditions() =
        River.PacketValidation {
            it.requireValue("@event_name", "adressebeskyttelse_endret")
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id", "fødselsnummer")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        mediator.mottaMeldingNy(
            melding =
                AdressebeskyttelseEndret(
                    id = packet["@id"].asUUID(),
                    fødselsnummer = packet["fødselsnummer"].asText(),
                    json = packet.toJson(),
                ),
            publiserer = MessageContextMeldingPubliserer(context),
        )
    }
}
