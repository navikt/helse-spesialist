package no.nav.helse.mediator.meldinger

import java.io.File
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndret
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class GosysOppgaveEndretRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: MeldingMediator
) : River.PacketListener {

    private fun erProd() = "prod-gcp" == System.getenv("NAIS_CLUSTER_NAME")

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
        val fødselsnummer = packet["fødselsnummer"].asText()
        if (ignorerliste.contains(fødselsnummer)) {
            sikkerlogg.warn("Ignorerer gosys_oppgave_endret for person $fødselsnummer")
        return
        }
        sikkerlogg.info("gosys_oppgave_endret for fnr {}", fødselsnummer)

        mediator.gosysOppgaveEndret(fødselsnummer, GosysOppgaveEndret(packet), context)
    }

}