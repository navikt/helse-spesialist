package no.nav.helse.kafka

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndret
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class GosysOppgaveEndretRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "gosys_oppgave_endret")
            it.requireKey("@id", "@opprettet", "fødselsnummer")
        }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerlogg.error("Forstod ikke gosys_oppgave_endret:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        mediator.mottaMelding(GosysOppgaveEndret(packet), context)
    }
}
