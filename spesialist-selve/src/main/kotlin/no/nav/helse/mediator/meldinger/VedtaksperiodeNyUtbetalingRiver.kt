package no.nav.helse.mediator.meldinger

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class VedtaksperiodeNyUtbetalingRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: HendelseMediator
) : River.PacketListener {
    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "vedtaksperiode_ny_utbetaling")
                it.requireKey("@id", "fødselsnummer", "vedtaksperiodeId", "utbetalingId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val fødselsnummer = packet["fødselsnummer"].asText()
        val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
        val utbetalingId = UUID.fromString(packet["utbetalingId"].asText())
        val id = UUID.fromString(packet["@id"].asText())

        sikkerlogg.info(
            "Mottok melding om vedtaksperiode_ny_utbetaling for {}, {}, {} som følge av melding med {}",
            StructuredArguments.kv("fødselsnummer", fødselsnummer),
            StructuredArguments.kv("vedtaksperiodeId", vedtaksperiodeId),
            StructuredArguments.kv("utbetalingId", utbetalingId),
            StructuredArguments.kv("id", id)
        )

        mediator.vedtaksperiodeNyUtbetaling(fødselsnummer, id, vedtaksperiodeId, utbetalingId, packet.toJson(), context)
    }
}