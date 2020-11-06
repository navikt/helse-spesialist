package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.OppgaveMediator
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.kommando.AvbrytForPersonCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.SlettVedtakCommand
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

internal class Tilbakerulling(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String,
    vedtaksperiodeIder: List<UUID>,
    commandContextDao: CommandContextDao,
    oppgaveMediator: OppgaveMediator,
    vedtakDao: VedtakDao
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        AvbrytForPersonCommand(fødselsnummer, oppgaveMediator, commandContextDao),
        SlettVedtakCommand(vedtaksperiodeIder, vedtakDao)
    )

    override fun fødselsnummer() = fødselsnummer
    override fun toJson() = json

    internal class TilbakerullingRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator
    ) : River.PacketListener {
        private val log = LoggerFactory.getLogger(this::class.java)
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "person_rullet_tilbake")
                    it.require("@id", ::uuid)
                    it.requireKey("fødselsnummer", "vedtaksperioderSlettet")
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
            sikkerLogg.error("Forstod ikke person_rullet_tilbake:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val id = UUID.fromString(packet["@id"].asText())
            log.info(
                "Mottok person_rullet_tilbake {}",
                keyValue("eventId", id)
            )
            mediator.tilbakerulling(
                packet,
                id,
                packet["fødselsnummer"].asText(),
                packet["vedtaksperioderSlettet"].map { UUID.fromString(it.asText()) },
                context
            )
        }
    }
}

private fun uuid(jsonNode: JsonNode): UUID = UUID.fromString(jsonNode.asText())
