package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.person.HentInfotrygdutbetalingerløsning
import no.nav.helse.spesialist.application.logg.loggInfo

class InfotrygdutbetalingerLøsningRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireValue("@event_name", "behov")
            it.requireValue("@final", true)
            it.requireAll("@behov", listOf("HentInfotrygdutbetalinger"))
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id", "contextId", "hendelseId")
            it.requireKey("@løsning.HentInfotrygdutbetalinger")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val hendelseId = packet["hendelseId"].asUUID()
        val contextId = packet["contextId"].asUUID()
        loggInfo(
            "Mottok HentInfotrygdutbetalinger for hendelseId: $hendelseId, contextId: $contextId",
        )
        mediator.løsning(
            hendelseId = hendelseId,
            contextId = contextId,
            behovId = packet["@id"].asUUID(),
            løsning = HentInfotrygdutbetalingerløsning(packet["@løsning.HentInfotrygdutbetalinger"]),
            kontekstbasertPubliserer = MessageContextMeldingPubliserer(context),
        )
    }
}
