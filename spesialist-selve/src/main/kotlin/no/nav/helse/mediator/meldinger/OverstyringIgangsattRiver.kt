package no.nav.helse.mediator.meldinger

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.modell.overstyring.OverstyringIgangsatt
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class OverstyringIgangsattRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "overstyring_igangsatt")
            it.requireKey("kilde")
            it.requireArray("berørtePerioder") {
                requireKey("vedtaksperiodeId")
            }
            it.requireKey("@id")
            it.requireKey("fødselsnummer")
        }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerLogg.error("Forstod ikke overstyring_igangsatt:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        mediator.mottaMelding(OverstyringIgangsatt(packet), context)
    }
}
