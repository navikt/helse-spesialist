package no.nav.helse.mediator.meldinger

import com.fasterxml.jackson.databind.JsonNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.IHendelseMediator
import no.nav.helse.modell.CommandContextDao
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.kommando.AvbrytCommand
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterSnapshotCommand
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.oppgave.OppgaveMediator
import no.nav.helse.rapids_rivers.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

internal class VedtaksperiodeForkastet(
    override val id: UUID,
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String,
    private val json: String,
    commandContextDao: CommandContextDao,
    vedtakDao: VedtakDao,
    warningDao: WarningDao,
    snapshotDao: SnapshotDao,
    oppgaveMediator: OppgaveMediator,
    speilSnapshotRestClient: SpeilSnapshotRestClient
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        AvbrytCommand(vedtaksperiodeId, commandContextDao, oppgaveMediator),
        OppdaterSnapshotCommand(speilSnapshotRestClient, vedtakDao, warningDao, snapshotDao, vedtaksperiodeId, fødselsnummer)
    )

    override fun fødselsnummer() = fødselsnummer
    override fun vedtaksperiodeId() = vedtaksperiodeId
    override fun toJson() = json

    internal class VedtaksperiodeForkastetRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: IHendelseMediator
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
