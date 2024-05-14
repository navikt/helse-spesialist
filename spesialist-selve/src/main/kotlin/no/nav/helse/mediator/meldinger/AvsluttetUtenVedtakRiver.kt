package no.nav.helse.mediator.meldinger

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.mediator.meldinger.hendelser.AvsluttetUtenVedtakMessage
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class AvsluttetUtenVedtakRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "avsluttet_uten_vedtak")
            it.requireKey("@id", "fødselsnummer", "aktørId", "vedtaksperiodeId", "organisasjonsnummer")
            it.requireKey("fom", "tom", "skjæringstidspunkt", "behandlingId")
            it.requireArray("hendelser")
        }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerlogg.error("Forstod ikke avsluttet_uten_vedtak:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        sikkerlogg.info("Mottok melding avsluttet_uten_vedtak:\n${packet.toJson()}")
        mediator.mottaMelding(AvsluttetUtenVedtakMessage(packet), context)
    }

    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }
}
