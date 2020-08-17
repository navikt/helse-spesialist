package no.nav.helse.mediator.kafka.meldinger

import kotliquery.Session
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.kafka.SpleisbehovMediator
import no.nav.helse.modell.SnapshotDao
import no.nav.helse.modell.VedtakDao
import no.nav.helse.modell.command.nyny.Command
import no.nav.helse.modell.command.nyny.OppdaterSnapshotCommand
import no.nav.helse.modell.vedtak.snapshot.SpeilSnapshotRestClient
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.util.*

internal class NyVedtaksperiodeEndretMessage(
    private val vedtaksperiodeId: UUID,
    private val fødselsnummer: String
) : Hendelse {

    override fun håndter(mediator: ICommandMediator) {
        mediator.håndter(this) // double dispatch
    }

    fun asCommand(session: Session, speilSnapshotRestClient: SpeilSnapshotRestClient): Command {
        return OppdaterSnapshotCommand(speilSnapshotRestClient, VedtakDao(session), SnapshotDao(session), vedtaksperiodeId, fødselsnummer)
    }

    internal class VedtaksperiodeEndretRiver(
        rapidsConnection: RapidsConnection,
        private val mediator: SpleisbehovMediator
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
            mediator.håndter(NyVedtaksperiodeEndretMessage(vedtaksperiodeId, fødselsnummer))
        }
    }
}
