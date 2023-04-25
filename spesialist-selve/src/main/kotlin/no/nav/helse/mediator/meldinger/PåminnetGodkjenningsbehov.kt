package no.nav.helse.mediator.meldinger

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.SørgForAtSnapshotErOppdatertCommand
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.spesialist.api.snapshot.SnapshotClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class PåminnetGodkjenningsbehov(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String,
    snapshotDao: SnapshotDao,
    snapshotClient: SnapshotClient,
) : Hendelse, MacroCommand() {

    override val commands = listOf(
        SørgForAtSnapshotErOppdatertCommand(
            snapshotDao = snapshotDao,
            snapshotClient = snapshotClient,
            fødselsnummer = fødselsnummer
        ),
    )

    override fun fødselsnummer() = fødselsnummer
    override fun toJson() = json

    internal class River(
        rapidsConnection: RapidsConnection,
        private val mediator: HendelseMediator,
    ) : no.nav.helse.rapids_rivers.River.PacketListener {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandAll("@behov", listOf("Godkjenning"))
                    it.rejectKey("@løsning")
                    it.requireKey(
                        "@id", "fødselsnummer", "vedtaksperiodeId", "utbetalingId"
                    )
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerLogg.error("Forstod ikke Godkjenning-behov:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val hendelseId = UUID.fromString(packet["@id"].asText())
            logg.info(
                "Sjekker om snapshot er utdatert for godkjenningsbehov med {}",
                keyValue("hendelseId", hendelseId)
            )
            mediator.påminnetGodkjenningsbehov(
                message = packet,
                id = hendelseId,
                fødselsnummer = packet["fødselsnummer"].asText(),
                vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText()),
                utbetalingId = UUID.fromString(packet["utbetalingId"].asText()),
                context = context
            )
        }
    }
}
