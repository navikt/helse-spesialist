package no.nav.helse.mediator.meldinger

import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.person.EndretEgenAnsattStatus
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class EndretSkjermetinfoRiver(
    rapidsConnection: RapidsConnection,
    private val meldingMediator: MeldingMediator,
) : River.PacketListener {
    private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", eventName)
                it.requireKey("@id", "fødselsnummer", "skjermet", "@opprettet")
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerLogg.error("Forstod ikke ${eventName}:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val fødselsnummer = packet["fødselsnummer"].asText()
        try {
            fødselsnummer.toLong()
        } catch (e: Exception) {
            sikkerLogg.warn("Mottok ugyldig fødselsnummer $fødselsnummer, skipper videre håndtering")
            return
        }

        meldingMediator.håndter(fødselsnummer, EndretEgenAnsattStatus(packet), context)
    }

    private companion object {
        private const val eventName = "endret_skjermetinfo"
    }
}
