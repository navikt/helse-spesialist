package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.api.graphql.SnapshotClient
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.kommando.AvbrytCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterSnapshotCommand
import no.nav.helse.modell.kommando.OppdaterSpeilSnapshotCommand
import no.nav.helse.modell.person.PersonDao
import no.nav.helse.spesialist.api.oppgave.OppgaveMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class VedtaksperiodeForkastet(
    override val id: UUID,
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String,
    private val json: String,
    commandContextDao: CommandContextDao,
    warningDao: WarningDao,
    oppgaveMediator: OppgaveMediator,
    snapshotClient: SnapshotClient,
    snapshotDao: SnapshotDao,
    personDao: PersonDao,
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        AvbrytCommand(vedtaksperiodeId, commandContextDao, oppgaveMediator),
        OppdaterSpeilSnapshotCommand(),
        OppdaterSnapshotCommand(
            snapshotClient = snapshotClient,
            snapshotDao = snapshotDao,
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            warningDao = warningDao,
            personDao = personDao,
            json = json
        )
    )

    override fun fødselsnummer() = fødselsnummer
    override fun vedtaksperiodeId() = vedtaksperiodeId
    override fun toJson() = json

    internal class VedtaksperiodeForkastetRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator
    ) : River.PacketListener {
        private val log = LoggerFactory.getLogger(this::class.java)
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        private companion object {
            private fun uuid(jsonNode: JsonNode): UUID = UUID.fromString(jsonNode.asText())
        }

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "vedtaksperiode_forkastet")
                    it.require("@id", ::uuid)
                    it.require("vedtaksperiodeId", ::uuid)
                    it.requireKey("fødselsnummer")
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerLogg.error("Forstod ikke vedtaksperiode_forkastet:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
            val id = UUID.fromString(packet["@id"].asText())
            log.info(
                "Mottok vedtaksperiode endret {}, {}",
                keyValue("vedtaksperiodeId", vedtaksperiodeId),
                keyValue("eventId", id)
            )
            mediator.vedtaksperiodeForkastet(packet, id, vedtaksperiodeId, packet["fødselsnummer"].asText(), context)
        }
    }
}
