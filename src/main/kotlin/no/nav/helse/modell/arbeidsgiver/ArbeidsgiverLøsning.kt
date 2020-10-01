package no.nav.helse.modell.arbeidsgiver

import no.nav.helse.mediator.kafka.meldinger.IHendelseMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.*

internal class ArbeidsgiverLøsning(private val navn: String) {
    internal fun oppdater(arbeidsgiverDao: ArbeidsgiverDao, orgnummer: String) =
        arbeidsgiverDao.updateNavn(orgnummer, navn)

    internal class ArbeidsgiverRiver(rapidsConnection: RapidsConnection,
                                     private val mediator: IHendelseMediator
    ) : River.PacketListener {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection)
                .apply {
                    validate {
                        it.demandValue("@event_name", "behov")
                        it.demandValue("@final", true)
                        it.demandAll("@behov", listOf("HentArbeidsgiverNavn"))
                        it.requireKey("contextId", "hendelseId")
                        it.requireKey("@løsning.HentArbeidsgiverNavn")
                    }
                }.register(this)
        }

        override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
            sikkerLog.error("forstod ikke HentArbeidsgiverNavn:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())
            val contextId = UUID.fromString(packet["contextId"].asText())
            mediator.løsning(hendelseId, contextId, ArbeidsgiverLøsning(packet["@løsning.HentArbeidsgiverNavn"].asText()), context)
        }
    }
}
