package no.nav.helse.mediator.meldinger

import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.mediator.meldinger.hendelser.AvviksvurderingMessage
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class AvviksvurderingRiver(
    rapidsConnection: RapidsConnection,
    private val mediator: HendelseMediator,
) : River.PacketListener {
    private companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "avviksvurdering")
                it.requireKey("@id", "fødselsnummer", "skjæringstidspunkt")
                it.requireKey(
                    "avviksvurdering.id",
                    "avviksvurdering.opprettet",
                    "avviksvurdering.beregningsgrunnlag",
                    "avviksvurdering.beregningsgrunnlag.totalbeløp",
                    "avviksvurdering.sammenligningsgrunnlag",
                    "avviksvurdering.sammenligningsgrunnlag.totalbeløp",
                    "avviksvurdering.sammenligningsgrunnlag.id",
                    "avviksvurdering.avviksprosent"
                )
                it.requireArray("avviksvurdering.beregningsgrunnlag.omregnedeÅrsinntekter") {
                    requireKey(
                        "arbeidsgiverreferanse",
                        "beløp"
                    )
                }
                it.requireArray("avviksvurdering.sammenligningsgrunnlag.innrapporterteInntekter") {
                    requireKey("arbeidsgiverreferanse")
                    requireArray("inntekter") {
                        requireKey(
                            "årMåned",
                            "beløp"
                        )
                    }
                }
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val hendelseId = UUID.fromString(packet["@id"].asText())
        logg.info(
            "Mottok avviksvurdering for {}",
            kv("hendelseId", hendelseId)
        )
        sikkerlogg.info(
            "Mottok avviksvurdering med {}, {}",
            kv("hendelseId", hendelseId),
            kv("hendelse", packet.toJson())
        )
        val message = AvviksvurderingMessage(packet)
        message.sendInnTil(mediator)
    }
}