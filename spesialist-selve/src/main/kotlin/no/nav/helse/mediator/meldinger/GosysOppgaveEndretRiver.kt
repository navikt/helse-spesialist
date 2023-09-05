package no.nav.helse.mediator.meldinger

import java.io.File
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.api.erProd
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class GosysOppgaveEndretRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: HendelseMediator,
    private val oppgaveDao: OppgaveDao,
    private val personDao: PersonDao
) : River.PacketListener {

    private val ignorerliste: Set<String> by lazy {
        if (erProd()) File("/var/run/configmaps/ignorere-oppgave-endret.csv").readText().split(",").toSet()
        else emptySet()
    }
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    init {
        River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "gosys_oppgave_endret")
                    it.requireKey("@id", "@opprettet", "fødselsnummer")
                }
            }.register(this)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerlogg.error("Forstod ikke gosys_oppgave_endret:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val id = UUID.fromString(packet["@id"].asText())
        val fødselsnummer = packet["fødselsnummer"].asText()
        val aktørId = personDao.finnAktørId(fødselsnummer) ?: kotlin.run {
            sikkerlogg.info("Finner ikke aktørid for person. Kjenner ikke til person med {}",
                StructuredArguments.kv("fødselsnummer", fødselsnummer)
            )
            return
        }
        if (ignorerliste.contains(fødselsnummer)) {
            sikkerlogg.warn("Ignorerer gosys_oppgave_endret for person $fødselsnummer")
        return
        }
        sikkerlogg.info("gosys_oppgave_endret for fnr {}", fødselsnummer)

        oppgaveDao.finnOppgaveId(fødselsnummer)?.also { oppgaveId ->
            sikkerlogg.info("Fant en oppgave for {}: {}", fødselsnummer, oppgaveId)
            val commandData = oppgaveDao.gosysOppgaveEndretCommandData(oppgaveId)
            if (commandData == null) {
                sikkerlogg.info("Fant ikke commandData for {} og {}", fødselsnummer, oppgaveId)
                return
            }

            sikkerlogg.info("Har oppgave til_godkjenning og commandData for fnr $fødselsnummer og vedtaksperiodeId ${commandData.vedtaksperiodeId}")
            mediator.gosysOppgaveEndret(id, fødselsnummer, aktørId, packet.toJson(), context)
        } ?: sikkerlogg.info("Ingen åpne oppgaver for {}", fødselsnummer)
    }

}