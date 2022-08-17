package no.nav.helse.mediator.meldinger

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.River.PacketListener
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class EndretSkjermetinfo(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String,
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = emptyList()

    override fun fødselsnummer(): String = fødselsnummer
    override fun vedtaksperiodeId(): UUID? = null
    override fun toJson(): String = json

    private companion object {
        const val eventName = "endret_skjermetinfo"
    }

    internal class River(
        rapidsConnection: RapidsConnection,
        private val personDao: PersonDao,
        private val egenAnsattDao: EgenAnsattDao,
    ) : PacketListener {
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
            sikkerLogg.error("Forstod ikke $eventName:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val id = UUID.fromString(packet["@id"].asText())
            val fødselsnummer = packet["fødselsnummer"].asText()
            try {
                fødselsnummer.toLong()
            } catch (e: Exception) {
                sikkerLogg.warn("Mottok ugyldig fødselsnummer $fødselsnummer, skipper videre håndtering")
                return
            }
            val erEgenAnsatt = packet["skjermet"].asBoolean()
            val opprettet = packet["@opprettet"]::asLocalDateTime.invoke()
            val logger = { msg: String ->
                sikkerLogg.info(msg, kv("fødselsnummer", fødselsnummer), kv("eventId", id))
            }
            when {
                personDao.findPersonByFødselsnummer(fødselsnummer) == null -> {
                    logger("Ignorerer $eventName for {} pga: person fins ikke i databasen, {}")
                }
                else -> {
                    logger("Mottok hendelse $eventName og oppdaterer database for {}, {}")
                    egenAnsattDao.lagre(fødselsnummer, erEgenAnsatt, opprettet)
                }
            }
        }
    }
}
