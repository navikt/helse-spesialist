package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.hendelser.AvvikVurdertMessage
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
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
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
        mediator.mottaMelding(AvvikVurdertMessage(packet), context)
    }
}
