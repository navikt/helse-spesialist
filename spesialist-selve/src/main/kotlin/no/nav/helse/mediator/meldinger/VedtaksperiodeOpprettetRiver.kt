package no.nav.helse.mediator.meldinger

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.api.erDev
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import org.slf4j.LoggerFactory

internal class VedtaksperiodeOpprettetRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: HendelseMediator
) : River.PacketListener {
    private val log = LoggerFactory.getLogger(this::class.java)

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "vedtaksperiode_opprettet")
                it.requireKey(
                    "@id", "fødselsnummer", "organisasjonsnummer", "vedtaksperiodeId", "fom", "tom", "skjæringstidspunkt"
                )
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val id = UUID.fromString(packet["@id"].asText())
        val fødselsnummer = packet["fødselsnummer"].asText()
        val organisasjonsnummer = packet["organisasjonsnummer"].asText()
        val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
        val fom = packet["fom"].asLocalDate()
        val tom = packet["tom"].asLocalDate()
        val skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate()

        log.info(
            "Mottok vedtaksperiode opprettet {}, {}",
            StructuredArguments.keyValue("vedtaksperiodeId", vedtaksperiodeId),
            StructuredArguments.keyValue("hendelseId", id),
        )
        if (erDev()) {
            try {
                organisasjonsnummer.toLong()
            } catch (exception: NumberFormatException) {
                log.warn("Mottok '$organisasjonsnummer' som organisasjonsnummer og konvertering til Long feilet. Skipper melding...")
                return
            }
        }
        mediator.vedtaksperiodeOpprettet(
            packet,
            id,
            fødselsnummer,
            organisasjonsnummer,
            vedtaksperiodeId,
            fom,
            tom,
            skjæringstidspunkt,
            context
        )
    }
}