package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.hendelser.AvsluttetUtenVedtakMessage

class AvsluttetUtenVedtakRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireValue("@event_name", "avsluttet_uten_vedtak")
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id", "fødselsnummer", "vedtaksperiodeId", "organisasjonsnummer")
            it.requireKey("skjæringstidspunkt", "behandlingId")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        mediator.mottaMelding(packet.hendelse(), MessageContextMeldingPubliserer(context))
    }

    private fun JsonMessage.hendelse() =
        AvsluttetUtenVedtakMessage(
            id = this["@id"].asUUID(),
            fødselsnummer = this["fødselsnummer"].asText(),
            vedtaksperiodeId = this["vedtaksperiodeId"].asUUID(),
            spleisBehandlingId = this["behandlingId"].asUUID(),
            json = this.toJson(),
        )
}
