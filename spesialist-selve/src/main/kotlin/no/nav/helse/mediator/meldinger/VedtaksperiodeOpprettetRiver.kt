package no.nav.helse.mediator.meldinger

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeOpprettet
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
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
                it.rejectValues("organisasjonsnummer", listOf("ARBEIDSLEDIG", "SELVSTENDIG", "FRILANS"))
                it.requireKey(
                    "@id", "fødselsnummer", "organisasjonsnummer", "vedtaksperiodeId", "fom", "tom", "skjæringstidspunkt"
                )
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        log.info(
            "Mottok vedtaksperiode opprettet {}, {}",
            keyValue("vedtaksperiodeId", UUID.fromString(packet["vedtaksperiodeId"].asText())),
            keyValue("hendelseId", UUID.fromString(packet["@id"].asText())),
        )
        val melding = VedtaksperiodeOpprettet(packet)
        mediator.håndter(melding.fødselsnummer(), melding, context)
    }
}
