package no.nav.helse.mediator.meldinger

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.egenansatt.EgenAnsattDao
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class EndretSkjermetinfoRiver(
    rapidsConnection: RapidsConnection,
    private val personDao: PersonDao,
    private val egenAnsattDao: EgenAnsattDao,
    val oppgaveDao: OppgaveDao,
    val godkjenningMediator: GodkjenningMediator,
) : River.PacketListener {
    private val logg: Logger = LoggerFactory.getLogger(this::class.java)
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
        val id = UUID.fromString(packet["@id"].asText())
        val fødselsnummer = packet["fødselsnummer"].asText()
        try {
            fødselsnummer.toLong()
        } catch (e: Exception) {
            sikkerLogg.warn("Mottok ugyldig fødselsnummer $fødselsnummer, skipper videre håndtering")
            return
        }

        val logger = { msg: String ->
            sikkerLogg.info(msg,
                StructuredArguments.kv("fødselsnummer", fødselsnummer),
                StructuredArguments.kv("eventId", id)
            )
        }
        if (personDao.findPersonByFødselsnummer(fødselsnummer) == null) {
            logger("Ignorerer $eventName for {} pga: person fins ikke i databasen, {}")
            return
        }

        val erEgenAnsatt = packet["skjermet"].asBoolean()
        val opprettet = packet["@opprettet"]::asLocalDateTime.invoke()

        logger("Mottok hendelse $eventName og oppdaterer database for {}, {}")
        egenAnsattDao.lagre(fødselsnummer, erEgenAnsatt, opprettet)
        if (erEgenAnsatt) avvisOppgave(fødselsnummer, context)
    }

    private fun avvisOppgave(fødselsnummer: String, context: MessageContext) {
        val oppgaveId = oppgaveDao.finnOppgaveId(fødselsnummer) ?: return

        logg.info("OppgaveId $oppgaveId avvises fordi vedkommende har blitt egen ansatt")
        val årsaker = listOf("Egen ansatt")
        godkjenningMediator.automatiskAvvisning(context::publish, årsaker, oppgaveId)
        oppgaveDao.invaliderOppgaveFor(fødselsnummer)
    }

    private companion object {
        const val eventName = "endret_skjermetinfo"
    }
}
