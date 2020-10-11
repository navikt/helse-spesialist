package no.nav.helse.mediator.meldinger

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.IHendelseMediator
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterSnapshotCommand
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

internal class VedtaksperiodeEndret(
    override val id: UUID,
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String,
    private val json: String,
    vedtakDao: VedtakDao,
    warningDao: WarningDao,
    snapshotDao: SnapshotDao,
    speilSnapshotRestClient: SpeilSnapshotRestClient
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        OppdaterSnapshotCommand(speilSnapshotRestClient, vedtakDao, warningDao, snapshotDao, vedtaksperiodeId, fødselsnummer)
    )

    override fun fødselsnummer() = fødselsnummer
    override fun vedtaksperiodeId() = vedtaksperiodeId
    override fun toJson() = json

    internal class VedtaksperiodeEndretRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: IHendelseMediator
    ) : River.PacketListener {

        private val log = LoggerFactory.getLogger(this::class.java)
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")

        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "vedtaksperiode_endret")
                    it.requireKey("vedtaksperiodeId")
                    it.requireKey("fødselsnummer")
                    it.requireKey("@id")
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
            sikkerLogg.error("Forstod ikke vedtaksperiode_endret:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
            val id = UUID.fromString(packet["@id"].asText())
            log.info(
                "Mottok vedtaksperiode endret {}, {}",
                keyValue("vedtaksperiodeId", vedtaksperiodeId),
                keyValue("eventId", id)
            )
            mediator.vedtaksperiodeEndret(packet, id, vedtaksperiodeId, packet["fødselsnummer"].asText(), context)
        }
    }
}
