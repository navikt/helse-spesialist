package no.nav.helse.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.asYearMonth
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.vilkårsprøving.AvviksvurderingBehovLøsning
import no.nav.helse.modell.vilkårsprøving.Beregningsgrunnlag
import no.nav.helse.modell.vilkårsprøving.InnrapportertInntekt
import no.nav.helse.modell.vilkårsprøving.Inntekt
import no.nav.helse.modell.vilkårsprøving.OmregnetÅrsinntekt
import no.nav.helse.modell.vilkårsprøving.Sammenligningsgrunnlag

class AvviksvurderingLøsningRiver(
    private val mediator: MeldingMediator,
) : SpesialistRiver {
    override fun preconditions(): River.PacketValidation {
        return River.PacketValidation {
            it.requireValue("@event_name", "behov")
            it.requireValue("@final", true)
            it.requireAll("@behov", listOf("Avviksvurdering"))
            it.requireKey("fødselsnummer", "contextId", "hendelseId", "@id")
        }
    }

    override fun validations(): River.PacketValidation {
        return River.PacketValidation {
            it.requireKey("@løsning.Avviksvurdering.avviksvurderingId")
            it.requireKey(
                "@løsning.Avviksvurdering.avviksprosent",
                "@løsning.Avviksvurdering.harAkseptabeltAvvik",
                "@løsning.Avviksvurdering.maksimaltTillattAvvik",
                "@løsning.Avviksvurdering.opprettet",
                "@løsning.Avviksvurdering.beregningsgrunnlag",
                "@løsning.Avviksvurdering.beregningsgrunnlag.totalbeløp",
                "@løsning.Avviksvurdering.sammenligningsgrunnlag",
                "@løsning.Avviksvurdering.sammenligningsgrunnlag.totalbeløp",
            )
            it.requireArray("@løsning.Avviksvurdering.beregningsgrunnlag.omregnedeÅrsinntekter") {
                requireKey("arbeidsgiverreferanse", "beløp")
            }

            it.requireArray("@løsning.Avviksvurdering.sammenligningsgrunnlag.innrapporterteInntekter") {
                requireKey("arbeidsgiverreferanse")
                requireArray("inntekter") {
                    requireKey("årMåned", "beløp")
                }
            }
        }
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        mediator.løsning(
            hendelseId = packet["hendelseId"].asUUID(),
            contextId = packet["contextId"].asUUID(),
            behovId = packet["@id"].asUUID(),
            løsning = løsning(packet),
            kontekstbasertPubliserer = MessageContextMeldingPubliserer(context),
        )
    }

    private fun løsning(packet: JsonMessage): AvviksvurderingBehovLøsning {
        return AvviksvurderingBehovLøsning(
            avviksvurderingId = packet["@løsning.Avviksvurdering.avviksvurderingId"].asUUID(),
            avviksprosent = packet["@løsning.Avviksvurdering.avviksprosent"].asDouble(),
            harAkseptabeltAvvik = packet["@løsning.Avviksvurdering.harAkseptabeltAvvik"].asBoolean(),
            maksimaltTillattAvvik = packet["@løsning.Avviksvurdering.maksimaltTillattAvvik"].asDouble(),
            opprettet = packet["@løsning.Avviksvurdering.opprettet"].asLocalDateTime(),
            beregningsgrunnlag = beregningsgrunnlag(packet["@løsning.Avviksvurdering.beregningsgrunnlag"]),
            sammenligningsgrunnlag = sammenligningsgrunnlag(packet["@løsning.Avviksvurdering.sammenligningsgrunnlag"]),
        )
    }

    private fun beregningsgrunnlag(json: JsonNode): Beregningsgrunnlag =
        Beregningsgrunnlag(
            totalbeløp = json["totalbeløp"].asDouble(),
            omregnedeÅrsinntekter = omregnedeÅrsinntekter(json["omregnedeÅrsinntekter"]),
        )

    private fun omregnedeÅrsinntekter(json: JsonNode): List<OmregnetÅrsinntekt> =
        json.map {
            OmregnetÅrsinntekt(
                arbeidsgiverreferanse = it["arbeidsgiverreferanse"].asText(),
                beløp = it["beløp"].asDouble(),
            )
        }

    private fun sammenligningsgrunnlag(json: JsonNode): Sammenligningsgrunnlag =
        Sammenligningsgrunnlag(
            totalbeløp = json["totalbeløp"].asDouble(),
            innrapporterteInntekter = innrapporterteInntekter(json["innrapporterteInntekter"]),
        )

    private fun innrapporterteInntekter(json: JsonNode): List<InnrapportertInntekt> =
        json.map {
            InnrapportertInntekt(
                arbeidsgiverreferanse = it["arbeidsgiverreferanse"].asText(),
                inntekter = inntekter(it["inntekter"]),
            )
        }

    private fun inntekter(json: JsonNode): List<Inntekt> =
        json.map {
            Inntekt(
                årMåned = it["årMåned"].asYearMonth(),
                beløp = it["beløp"].asDouble(),
            )
        }
}
