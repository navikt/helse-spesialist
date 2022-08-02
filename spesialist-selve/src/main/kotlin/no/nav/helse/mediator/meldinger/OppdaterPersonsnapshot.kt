package no.nav.helse.mediator.meldinger

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterSnapshotUtenÅLagreWarningsCommand
import no.nav.helse.rapids_rivers.*
import no.nav.helse.rapids_rivers.River.PacketListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import no.nav.helse.mediator.api.graphql.SnapshotClient
import no.nav.helse.modell.SnapshotDao

internal class OppdaterPersonsnapshot(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String,
    snapshotClient: SnapshotClient,
    snapshotDao: SnapshotDao
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        OppdaterSnapshotUtenÅLagreWarningsCommand(
            fødselsnummer = fødselsnummer,
            snapshotClient = snapshotClient,
            snapshotDao = snapshotDao
        )
    )

    override fun fødselsnummer(): String = fødselsnummer
    override fun vedtaksperiodeId(): UUID? = null
    override fun toJson(): String = json

    internal class River(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator
    ) : PacketListener {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "oppdater_personsnapshot")
                    it.requireKey("@id", "fødselsnummer")
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerLogg.error("Forstod ikke oppdater_personsnapshot:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val id = UUID.fromString(packet["@id"].asText())
            val fødselsnummer = packet["fødselsnummer"].asText()
            sikkerLogg.info(
                "Mottok forespørsel om å oppdatere personsnapshot på {}, {}",
                keyValue("fødselsnummer", fødselsnummer),
                keyValue("eventId", id)
            )
            mediator.oppdaterPersonsnapshot(packet, context)
        }
    }
}
