package no.nav.helse.kafka

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.person.KlargjørTilgangsrelaterteData
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class KlargjørPersonForVisningRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "klargjør_person_for_visning")
            it.requireKey("@id", "fødselsnummer")
        }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerlogg.error("Forstod ikke klargjør_person_for_visning:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        mediator.mottaMelding(KlargjørTilgangsrelaterteData(packet), context)
    }
}
