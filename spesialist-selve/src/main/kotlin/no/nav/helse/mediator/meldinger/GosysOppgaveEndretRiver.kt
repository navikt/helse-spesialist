package no.nav.helse.mediator.meldinger

import no.nav.helse.bootstrap.Environment
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.modell.gosysoppgaver.GosysOppgaveEndret
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.io.File

internal class GosysOppgaveEndretRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private val env = Environment()

    private val ignorerliste: Set<String> by lazy {
        if (env.erProd) {
            File("/var/run/configmaps/ignorere-oppgave-endret.csv").readText().split(",").toSet()
        } else {
            emptySet()
        }
    }
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "gosys_oppgave_endret")
            it.requireKey("@id", "@opprettet", "fødselsnummer")
        }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerlogg.error("Forstod ikke gosys_oppgave_endret:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val fødselsnummer = packet["fødselsnummer"].asText()
        if (ignorerliste.contains(fødselsnummer)) {
            sikkerlogg.warn("Ignorerer gosys_oppgave_endret for person $fødselsnummer")
            return
        }
        sikkerlogg.info("gosys_oppgave_endret for fnr {}", fødselsnummer)

        mediator.håndter(GosysOppgaveEndret(packet), context)
    }
}
