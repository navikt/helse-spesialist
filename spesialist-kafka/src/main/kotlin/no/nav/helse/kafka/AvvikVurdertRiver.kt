package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.asYearMonth
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.mediator.meldinger.hendelser.AvvikVurdertMessage
import no.nav.helse.modell.vilkårsprøving.BeregningsgrunnlagDto
import no.nav.helse.modell.vilkårsprøving.InnrapportertInntektDto
import no.nav.helse.modell.vilkårsprøving.InntektDto
import no.nav.helse.modell.vilkårsprøving.OmregnetÅrsinntektDto
import no.nav.helse.modell.vilkårsprøving.SammenligningsgrunnlagDto

class AvvikVurdertRiver(
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
        mediator.mottaMelding(
            AvvikVurdertMessage(
                unikId = packet["avviksvurdering.id"].asUUID(),
                vilkårsgrunnlagId =
                    packet["avviksvurdering.vilkårsgrunnlagId"].takeUnless { it.isMissingOrNull() }
                        ?.asUUID(),
                fødselsnummer = packet["fødselsnummer"].asText(),
                vedtaksperiodeId = packet["vedtaksperiodeId"].asUUID(),
                skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate(),
                opprettet = packet["avviksvurdering.opprettet"].asLocalDateTime(),
                avviksprosent = packet["avviksvurdering.avviksprosent"].asDouble(),
                beregningsgrunnlag = beregningsgrunnlag(packet["avviksvurdering.beregningsgrunnlag"]),
                sammenligningsgrunnlag = sammenligningsgrunnlag(packet["avviksvurdering.sammenligningsgrunnlag"]),
                id = packet["@id"].asUUID(),
                json = packet.toJson(),
            ),
            MessageContextMeldingPubliserer(context),
        )
    }

    private fun beregningsgrunnlag(json: JsonNode): BeregningsgrunnlagDto =
        BeregningsgrunnlagDto(
            totalbeløp = json["totalbeløp"].asDouble(),
            omregnedeÅrsinntekter = omregnedeÅrsinntekter(json["omregnedeÅrsinntekter"]),
        )

    private fun omregnedeÅrsinntekter(json: JsonNode): List<OmregnetÅrsinntektDto> =
        json.map {
            OmregnetÅrsinntektDto(
                arbeidsgiverreferanse = it["arbeidsgiverreferanse"].asText(),
                beløp = it["beløp"].asDouble(),
            )
        }

    private fun sammenligningsgrunnlag(json: JsonNode): SammenligningsgrunnlagDto =
        SammenligningsgrunnlagDto(
            totalbeløp = json["totalbeløp"].asDouble(),
            innrapporterteInntekter = innrapporterteInntekter(json["innrapporterteInntekter"]),
        )

    private fun innrapporterteInntekter(json: JsonNode): List<InnrapportertInntektDto> =
        json.map {
            InnrapportertInntektDto(
                arbeidsgiverreferanse = it["arbeidsgiverreferanse"].asText(),
                inntekter = inntekter(it["inntekter"]),
            )
        }

    private fun inntekter(json: JsonNode): List<InntektDto> =
        json.map {
            InntektDto(
                årMåned = it["årMåned"].asYearMonth(),
                beløp = it["beløp"].asDouble(),
            )
        }
}
