package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.kafka.meldinger.IHendelseMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.*

internal class HentInfotrygdutbetalingerLøsning(private val utbetalinger: JsonNode) {

    internal fun lagre(personDao: PersonDao) =
        personDao.insertInfotrygdutbetalinger(utbetalinger)

    fun oppdater(personDao: PersonDao, fødselsnummer: String) {
        if (personDao.findInfotrygdutbetalinger(fødselsnummer) != null) {
            personDao.updateInfotrygdutbetalinger(fødselsnummer, utbetalinger)
        } else {
            val utbetalingRef = personDao.insertInfotrygdutbetalinger(utbetalinger)
            personDao.updateInfotrygdutbetalingerRef(fødselsnummer, utbetalingRef)
        }
    }

    internal class InfotrygdutbetalingerRiver(rapidsConnection: RapidsConnection, private val mediator: IHendelseMediator) : River.PacketListener {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
        init {
            River(rapidsConnection)
                .apply {  validate {
                    it.demandValue("@event_name", "behov")
                    it.demandValue("@final", true)
                    it.demandAll("@behov", listOf("HentInfotrygdutbetalinger"))
                    it.requireKey("contextId", "hendelseId")
                    it.requireKey("@løsning.HentInfotrygdutbetalinger")
                }
                }.register(this)
        }

        override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
            sikkerLog.error("forstod ikke HentInfotrygdutbetalinger:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())
            val contextId = UUID.fromString(packet["contextId"].asText())
            mediator.løsning(hendelseId, contextId, HentInfotrygdutbetalingerLøsning(packet["@løsning.HentInfotrygdutbetalinger"]), context)
        }
    }
}
