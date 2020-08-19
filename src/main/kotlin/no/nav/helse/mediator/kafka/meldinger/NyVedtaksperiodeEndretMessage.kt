package no.nav.helse.mediator.kafka.meldinger

import com.fasterxml.jackson.databind.JsonNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.command.nyny.Command
import no.nav.helse.modell.command.nyny.CommandContext
import no.nav.helse.modell.command.nyny.OppdaterSnapshotCommand
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.*

internal class NyVedtaksperiodeEndretMessage(
    override val id: UUID,
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String
) : Hendelse {

    constructor(json: JsonNode) : this(
        id = UUID.fromString(json.path("id").asText()),
        vedtaksperiodeId = UUID.fromString(json.path("vedtaksperiodeId").asText()),
        fødselsnummer = json.path("fødselsnummer").asText()
    )

    override fun håndter(mediator: ICommandMediator, context: CommandContext) {
        mediator.håndter(this, context) // double dispatch
    }

    override fun fødselsnummer(): String {
        return fødselsnummer
    }

    fun asCommand(vedtakDao: VedtakDao, snapshotDao: SnapshotDao, speilSnapshotRestClient: SpeilSnapshotRestClient): Command {
        return OppdaterSnapshotCommand(speilSnapshotRestClient, vedtakDao, snapshotDao, vedtaksperiodeId, fødselsnummer)
    }

    override fun toJson(): String {
        return """{
    "id": "$id",
    "vedtaksperiodeId": "$vedtaksperiodeId",
    "fødselsnummer": "$fødselsnummer"
}""".trimIndent()
    }

    internal class VedtaksperiodeEndretRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: IHendelseMediator
    ) : River.PacketListener {

        private val log = LoggerFactory.getLogger(this::class.java)

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

        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
            val fødselsnummer = packet["fødselsnummer"].asText()
            val id = UUID.fromString(packet["@id"].asText())
            log.info(
                "Mottok vedtaksperiode endret {}, {}",
                keyValue("vedtaksperiodeId", vedtaksperiodeId),
                keyValue("eventId", id)
            )
            mediator.håndter(NyVedtaksperiodeEndretMessage(id, vedtaksperiodeId, fødselsnummer))
        }
    }
}
