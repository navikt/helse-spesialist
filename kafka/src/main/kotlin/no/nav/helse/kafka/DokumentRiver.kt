package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.spesialist.application.logg.loggInfo

class DokumentRiver(
    private val meldingMediator: MeldingMediator,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireValue("@event_name", "hent-dokument")
            it.requireKey("@løsning.dokument")
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id", "fødselsnummer", "dokumentId")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val fødselsnummer = packet["fødselsnummer"].asText()
        val dokumentId = packet["dokumentId"].asUUID()
        val dokument = packet["@løsning.dokument"]

        loggInfo(
            "Mottok hendelse hent-dokument og lagrer dokumentet",
            "dokumentId" to dokumentId,
        )

        meldingMediator.mottaDokument(fødselsnummer, dokumentId, dokument)
    }
}
