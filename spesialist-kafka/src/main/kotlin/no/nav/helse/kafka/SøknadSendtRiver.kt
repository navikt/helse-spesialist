package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.person.SøknadSendt
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SøknadSendtRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val logg = LoggerFactory.getLogger(this::class.java)
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireAny(
                "@event_name",
                listOf("sendt_søknad_arbeidsgiver", "sendt_søknad_nav", "sendt_søknad_arbeidsledig"),
            )
        }
    }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id", "fnr", "aktorId")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        logg.info(
            "Mottok {} med {}",
            keyValue("hendelse", packet["@event_name"].asText()),
            keyValue("hendelseId", packet["@id"].asUUID()),
        )
        sikkerLogg.info(
            "Mottok {} med {}, {}",
            keyValue("hendelse", packet["@event_name"].asText()),
            keyValue("hendelseId", packet["@id"].asUUID()),
            keyValue("hendelse", packet.toJson()),
        )
        mediator.mottaSøknadSendt(
            melding = søknadSendt(packet),
            kontekstbasertPubliserer = MessageContextMeldingPubliserer(context),
        )
    }

    private fun søknadSendt(packet: JsonMessage) =
        SøknadSendt(
            id = packet["@id"].asUUID(),
            fødselsnummer = packet["fnr"].asText(),
            aktørId = packet["aktorId"].asText(),
            json = packet.toJson(),
        )
}
