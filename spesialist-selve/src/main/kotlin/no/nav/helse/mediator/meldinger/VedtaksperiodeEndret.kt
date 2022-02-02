package no.nav.helse.mediator.meldinger

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.IHendelseMediator
import no.nav.helse.mediator.api.graphql.SpeilSnapshotGraphQLClient
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.SpeilSnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.WarningDao
import no.nav.helse.modell.kommando.Command
import no.nav.helse.modell.kommando.MacroCommand
import no.nav.helse.modell.kommando.OppdaterSnapshotCommand
import no.nav.helse.modell.kommando.OppdaterSpeilSnapshotCommand
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.rapids_rivers.*
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
    speilSnapshotDao: SpeilSnapshotDao,
    speilSnapshotRestClient: SpeilSnapshotRestClient,
    snapshotDao: SnapshotDao,
    speilSnapshotGraphQLClient: SpeilSnapshotGraphQLClient
) : Hendelse, MacroCommand() {
    override val commands: List<Command> = listOf(
        OppdaterSpeilSnapshotCommand(
            speilSnapshotRestClient = speilSnapshotRestClient,
            vedtakDao = vedtakDao,
            warningDao = warningDao,
            speilSnapshotDao = speilSnapshotDao,
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer
        ),
        OppdaterSnapshotCommand(
            speilSnapshotGraphQLClient = speilSnapshotGraphQLClient,
            vedtakDao = vedtakDao,
            snapshotDao = snapshotDao,
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            warningDao = warningDao
        )
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
                    it.interestedIn("aktørId")
                    it.requireKey("@id")
                }
            }.register(this)
        }

        override fun onError(problems: MessageProblems, context: MessageContext) {
            sikkerLogg.error("Forstod ikke vedtaksperiode_endret:\n${problems.toExtendedReport()}")
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
            val id = UUID.fromString(packet["@id"].asText())
            if (packet["aktørId"].asText() == "1000063751749") {
                log.info("Ignorerer vedtaksperiode_endret for aktørId ${packet["aktørId"].asText()}")
                return
            }
            log.info(
                "Mottok vedtaksperiode endret {}, {}",
                keyValue("vedtaksperiodeId", vedtaksperiodeId),
                keyValue("eventId", id)
            )
            mediator.vedtaksperiodeEndret(packet, id, vedtaksperiodeId, packet["fødselsnummer"].asText(), context)
        }
    }
}
