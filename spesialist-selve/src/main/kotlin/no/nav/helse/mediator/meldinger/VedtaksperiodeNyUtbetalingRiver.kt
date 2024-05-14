package no.nav.helse.mediator.meldinger

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeNyUtbetaling
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

internal class VedtaksperiodeNyUtbetalingRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private companion object {
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "vedtaksperiode_ny_utbetaling")
            it.requireKey("@id", "fødselsnummer", "vedtaksperiodeId", "utbetalingId")
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val fødselsnummer = packet["fødselsnummer"].asText()
        val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
        val utbetalingId = UUID.fromString(packet["utbetalingId"].asText())
        val id = UUID.fromString(packet["@id"].asText())

        sikkerlogg.info(
            "Mottok melding om vedtaksperiode_ny_utbetaling for {}, {}, {} som følge av melding med {}",
            kv("fødselsnummer", fødselsnummer),
            kv("vedtaksperiodeId", vedtaksperiodeId),
            kv("utbetalingId", utbetalingId),
            kv("id", id),
        )

        mediator.mottaMelding(VedtaksperiodeNyUtbetaling(packet), context)
    }
}
