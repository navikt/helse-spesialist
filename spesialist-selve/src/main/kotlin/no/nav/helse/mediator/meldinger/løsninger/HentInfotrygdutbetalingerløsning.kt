package no.nav.helse.mediator.meldinger.løsninger

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class HentInfotrygdutbetalingerløsning(private val utbetalinger: JsonNode) {

    internal fun lagre(personDao: PersonDao): Long =
        personDao.insertInfotrygdutbetalinger(utbetalinger)

    fun oppdater(personDao: PersonDao, fødselsnummer: String) {
        personDao.upsertInfotrygdutbetalinger(fødselsnummer, utbetalinger)
    }

    internal class InfotrygdutbetalingerRiver(rapidsConnection: RapidsConnection, private val mediator: HendelseMediator) : River.PacketListener {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
        init {
            River(rapidsConnection)
                .apply {  validate {
                    it.demandValue("@event_name", "behov")
                    it.demandValue("@final", true)
                    it.demandAll("@behov", listOf("HentInfotrygdutbetalinger"))
                    it.requireKey("@id", "contextId", "hendelseId")
                    it.requireKey("@løsning.HentInfotrygdutbetalinger")
                }
                }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerLog.error("forstod ikke HentInfotrygdutbetalinger:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val hendelseId = UUID.fromString(packet["hendelseId"].asText())
            val contextId = UUID.fromString(packet["contextId"].asText())
            sikkerLog.info(
                "Mottok HentInfotrygdutbetalinger for {}, {}",
                kv("hendelseId", hendelseId),
                kv("contextId", contextId)
            )
            mediator.løsning(
                hendelseId,
                contextId,
                UUID.fromString(packet["@id"].asText()),
                HentInfotrygdutbetalingerløsning(packet["@løsning.HentInfotrygdutbetalinger"]),
                context
            )
        }
    }
}
