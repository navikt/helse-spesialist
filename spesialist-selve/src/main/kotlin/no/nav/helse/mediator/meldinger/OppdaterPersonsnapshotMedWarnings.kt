package no.nav.helse.mediator.meldinger

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.IHendelseMediator
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.rapids_rivers.*
import no.nav.helse.rapids_rivers.River.PacketListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import no.nav.helse.mediator.api.graphql.SnapshotClient
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.kommando.OppdaterSnapshotMedWarningsCommand
import no.nav.helse.modell.person.PersonDao

internal class OppdaterPersonsnapshotMedWarnings(
    override val id: UUID,
    private val fødselsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val json: String,
    snapshotClient: SnapshotClient,
    snapshotDao: SnapshotDao,
    warningDao: WarningDao,
    personDao: PersonDao,
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        OppdaterSnapshotMedWarningsCommand(
            fødselsnummer = fødselsnummer,
            snapshotClient = snapshotClient,
            snapshotDao = snapshotDao,
            vedtaksperiodeId = vedtaksperiodeId,
            warningDao = warningDao,
            personDao = personDao
        )
    )

    override fun fødselsnummer(): String = fødselsnummer
    override fun vedtaksperiodeId(): UUID = vedtaksperiodeId
    override fun toJson(): String = json

    internal class River(
        rapidsConnection: RapidsConnection,
        private val mediator: IHendelseMediator,
    ) : PacketListener {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "oppdater_personsnapshot_med_warnings")
                    it.requireKey("@id", "fødselsnummer", "vedtaksperiodeId")
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerLogg.error("Forstod ikke oppdater_personsnapshot_med_warnings:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val id = UUID.fromString(packet["@id"].asText())
            val fødselsnummer = packet["fødselsnummer"].asText()
            val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
            sikkerLogg.info(
                "Mottok forespørsel om å oppdatere personsnapshot med warnings på fødselsnummer {} og vedtaksperiodeid {}, {}",
                keyValue("fødselsnummer", fødselsnummer),
                keyValue("vedtaksperiodeId", vedtaksperiodeId),
                keyValue("eventId", id)
            )
            mediator.oppdaterPersonsnapshotMedWarnings(packet, fødselsnummer, context)
        }
    }
}
