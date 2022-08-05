package no.nav.helse.mediator.meldinger

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.egenansatt.EgenAnsattCommand
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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class InnhentSkjermetinfo(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String,
    egenAnsattDao: EgenAnsattDao,
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        EgenAnsattCommand(
            egenAnsattDao = egenAnsattDao,
        )
    )

    override fun fødselsnummer(): String = fødselsnummer
    override fun vedtaksperiodeId(): UUID? = null
    override fun toJson(): String = json

    internal class River(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator,
        private val personDao: PersonDao,
    ) : PacketListener {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "innhent_skjermetinfo")
                    it.requireKey("@id", "fødselsnummer")
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerLogg.error("Forstod ikke innhent_skjermetinfo:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val id = UUID.fromString(packet["@id"].asText())
            val fødselsnummer = packet["fødselsnummer"].asText()
            val logger = { msg: String ->
                sikkerLogg.info(msg, kv("fødselsnummer", fødselsnummer), kv("eventId", id))
            }
            when {
                personDao.findPersonByFødselsnummer(fødselsnummer) == null -> {
                    logger("Ignorerer innhent_skjermetinfo for {} pga: person fins ikke i databasen, {}")
                }
                else -> {
                    logger("Mottok hendelse innhent_skjermetinfo for {}, {}")
                    mediator.innhentSkjermetinfo(packet, context)
                }
            }
        }
    }
}
