package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.meldinger.hendelser.AvvikVurdertMessage

internal class AvvikVurdertRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireValue("@event_name", "avvik_vurdert")
        }
    }

    override fun validations() =
        River.PacketValidation {
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
        mediator.mottaMelding(AvvikVurdertMessage(packet), context)
    }
}
