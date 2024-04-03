package no.nav.helse.mediator.meldinger.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.avviksvurdering.Avviksvurdering
import no.nav.helse.modell.avviksvurdering.BeregningsgrunnlagDto
import no.nav.helse.modell.avviksvurdering.InnrapportertInntektDto
import no.nav.helse.modell.avviksvurdering.InntektDto
import no.nav.helse.modell.avviksvurdering.OmregnetÅrsinntektDto
import no.nav.helse.modell.avviksvurdering.SammenligningsgrunnlagDto
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.asYearMonth
import no.nav.helse.rapids_rivers.isMissingOrNull

class AvvikVurdertMessage(packet: JsonMessage) {
    private val unikId = packet["avviksvurdering.id"].asUUID()
    private val vilkårsgrunnlagId = packet["avviksvurdering.vilkårsgrunnlagId"].takeUnless { it.isMissingOrNull() }?.asUUID()
    private val fødselsnummer = packet["fødselsnummer"].asText()
    private val skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate()
    private val opprettet = packet["avviksvurdering.opprettet"].asLocalDateTime()
    private val avviksprosent = packet["avviksvurdering.avviksprosent"].asDouble()
    private val beregningsgrunnlag = beregningsgrunnlag(packet["avviksvurdering.beregningsgrunnlag"])
    private val sammenligningsgrunnlag = sammenligningsgrunnlag(packet["avviksvurdering.sammenligningsgrunnlag"])

    private val avviksvurdering
        get() =
            Avviksvurdering(
                unikId = unikId,
                vilkårsgrunnlagId = vilkårsgrunnlagId,
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                opprettet = opprettet,
                avviksprosent = avviksprosent,
                sammenligningsgrunnlag = sammenligningsgrunnlag,
                beregningsgrunnlag = beregningsgrunnlag,
            ).toDto()

    internal fun sendInnTil(mediator: MeldingMediator) {
        mediator.håndter(avviksvurdering)
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
