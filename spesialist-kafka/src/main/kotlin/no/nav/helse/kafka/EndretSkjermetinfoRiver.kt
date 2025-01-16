package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.person.EndretEgenAnsattStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class EndretSkjermetinfoRiver(
    private val meldingMediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")

    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireValue("@event_name", "endret_skjermetinfo")
        }
    }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id", "fødselsnummer", "skjermet", "@opprettet")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val fødselsnummer = packet["fødselsnummer"].asText()
        try {
            fødselsnummer.toLong()
        } catch (e: Exception) {
            sikkerlogg.warn("Mottok ugyldig fødselsnummer $fødselsnummer, skipper videre håndtering")
            return
        }

        meldingMediator.mottaMelding(
            EndretEgenAnsattStatus(
                id = packet["@id"].asUUID(),
                fødselsnummer = packet["fødselsnummer"].asText(),
                erEgenAnsatt = packet["skjermet"].asBoolean(),
                opprettet = packet["@opprettet"].asLocalDateTime(),
                json = packet.toJson(),
            ),
            MessageContextMeldingPubliserer(context),
        )
    }
}
