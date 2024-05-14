package no.nav.helse.mediator.meldinger

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.modell.vedtaksperiode.BehandlingOpprettet
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River

internal class BehandlingOpprettetRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "behandling_opprettet")
            it.rejectValues("organisasjonsnummer", listOf("ARBEIDSLEDIG", "SELVSTENDIG", "FRILANS"))
            it.requireKey(
                "@id",
                "vedtaksperiodeId",
                "behandlingId",
                "fødselsnummer",
                "organisasjonsnummer",
            )
            it.requireKey("fom", "tom")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        mediator.mottaMelding(BehandlingOpprettet(packet), context)
    }
}
