package no.nav.helse.mediator.meldinger

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.modell.person.EndretEgenAnsattStatus
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class EndretSkjermetinfoRiver(
    private val meldingMediator: MeldingMediator,
) : SpesialistRiver {
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", EVENT_NAME)
            it.requireKey("@id", "fødselsnummer", "skjermet", "@opprettet")
        }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerLogg.error("Forstod ikke $EVENT_NAME:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val fødselsnummer = packet["fødselsnummer"].asText()
        try {
            fødselsnummer.toLong()
        } catch (e: Exception) {
            sikkerLogg.warn("Mottok ugyldig fødselsnummer $fødselsnummer, skipper videre håndtering")
            return
        }

        meldingMediator.mottaMelding(EndretEgenAnsattStatus(packet), context)
    }

    private companion object {
        private const val EVENT_NAME = "endret_skjermetinfo"
    }
}
