package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.person.HentInfotrygdutbetalingerløsning
import org.slf4j.LoggerFactory

class InfotrygdutbetalingerLøsningRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireValue("@event_name", "behov")
            it.requireValue("@final", true)
            it.requireAll("@behov", listOf("HentInfotrygdutbetalinger"))
        }
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
        sikkerlogg.info(
            "Mottok HentInfotrygdutbetalinger for {}, {}",
            kv("hendelseId", hendelseId),
            kv("contextId", contextId),
        )
        mediator.løsning(
            hendelseId = hendelseId,
            contextId = contextId,
            behovId = packet["@id"].asUUID(),
            løsning = HentInfotrygdutbetalingerløsning(packet["@løsning.HentInfotrygdutbetalinger"]),
            publiserer = MessageContextMeldingPubliserer(context),
        )
    }
}
