package no.nav.helse.mediator.meldinger

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.SpesialistRiver
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.hendelser.AvvikVurdertMessage
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class AvvikVurdertRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    private companion object {
        private val logg = LoggerFactory.getLogger(this::class.java)
        private val sikkerlogg: Logger = LoggerFactory.getLogger("tjenestekall")
    }

    override fun validations() =
        River.PacketValidation {
            it.demandValue("@event_name", "avvik_vurdert")
            it.requireKey("@id", "fødselsnummer", "skjæringstidspunkt", "vedtaksperiodeId")
            it.interestedIn("avviksvurdering.vilkårsgrunnlagId")
            it.requireKey(
                "avviksvurdering.id",
                "avviksvurdering.opprettet",
                "avviksvurdering.beregningsgrunnlag",
                "avviksvurdering.beregningsgrunnlag.totalbeløp",
                "avviksvurdering.sammenligningsgrunnlag",
                "avviksvurdering.sammenligningsgrunnlag.totalbeløp",
                "avviksvurdering.avviksprosent",
            )
            it.requireArray("avviksvurdering.beregningsgrunnlag.omregnedeÅrsinntekter") {
                requireKey(
                    "arbeidsgiverreferanse",
                    "beløp",
                )
            }
            it.requireArray("avviksvurdering.sammenligningsgrunnlag.innrapporterteInntekter") {
                requireKey("arbeidsgiverreferanse")
                requireArray("inntekter") {
                    requireKey(
                        "årMåned",
                        "beløp",
                    )
                }
            }
        }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val hendelseId = packet["@id"].asUUID()
        logg.info(
            "Mottok avvik_vurdert for {}",
            kv("hendelseId", hendelseId),
        )
        sikkerlogg.info(
            "Mottok avvik_vurdert med {}, {}",
            kv("hendelseId", hendelseId),
            kv("hendelse", packet.toJson()),
        )
        val message = AvvikVurdertMessage(packet)
        // message.sendInnTil(mediator)
        mediator.mottaMelding(
            message,
            context,
        )
    }
}
