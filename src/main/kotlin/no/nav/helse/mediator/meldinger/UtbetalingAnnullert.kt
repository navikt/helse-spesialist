package no.nav.helse.mediator.meldinger

import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.IHendelseMediator
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterSnapshotV2Command
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.River.PacketListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

internal class UtbetalingAnnullert(
    override val id: UUID,
    private val fødselsnummer: String,
    private val json: String,
    speilSnapshotRestClient: SpeilSnapshotRestClient,
    vedtakDao: VedtakDao
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        OppdaterSnapshotV2Command(speilSnapshotRestClient = speilSnapshotRestClient, vedtakDao = vedtakDao, fødselsnummer = fødselsnummer)
    )
    override fun fødselsnummer(): String = fødselsnummer
    override fun vedtaksperiodeId(): UUID? = null
    override fun toJson(): String = json

    internal class River(
        rapidsConnection: RapidsConnection,
        private val mediator: IHendelseMediator
    ) : PacketListener {
        private val log = LoggerFactory.getLogger("UtbetalingAnnullert")
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "utbetaling_annullert")
                    it.requireKey("@id", "fødselsnummer", "fagsystemId")
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
            sikkerLogg.error("Forstod ikke utbetaling_annullert:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val id = UUID.fromString(packet["@id"].asText())
            val fagsystemId = packet["fagsystemId"].asText()
            log.info(
                "Mottok utbetaling annullert {}, {}",
                StructuredArguments.keyValue("fagsystemId", fagsystemId),
                StructuredArguments.keyValue("eventId", id)
            )
            mediator.utbetalingAnnullert(packet, context)
        }
    }
}
