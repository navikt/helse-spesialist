package no.nav.helse.mediator.meldinger.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.avviksvurdering.Avviksvurdering
import no.nav.helse.modell.avviksvurdering.Beregningsgrunnlag
import no.nav.helse.modell.avviksvurdering.InnrapportertInntekt
import no.nav.helse.modell.avviksvurdering.Inntekt
import no.nav.helse.modell.avviksvurdering.OmregnetÅrsinntekt
import no.nav.helse.modell.avviksvurdering.Sammenligningsgrunnlag
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.asYearMonth

class AvviksvurderingMessage(packet: JsonMessage) {

    private val fødselsnummer = packet["fødselsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()
    private val skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate()
    private val opprettet = packet["avviksvurdering.opprettet"].asLocalDateTime()
    private val avviksprosent = packet["avviksvurdering.avviksprosent"].asDouble()
    private val beregningsgrunnlag = beregningsgrunnlag(packet["avviksvurdering.beregningsgrunnlag"])
    private val sammenligningsgrunnlag = sammenligningsgrunnlag(packet["avviksvurdering.sammenligningsgrunnlag"])


    private val avviksvurdering
        get() = Avviksvurdering(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            skjæringstidspunkt = skjæringstidspunkt,
            oppretttet = opprettet,
            avviksprosent = avviksprosent,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            beregningsgrunnlag = beregningsgrunnlag
        )

    internal fun sendInnTil(mediator: HendelseMediator) {
        mediator.håndter(avviksvurdering)
    }

    private fun beregningsgrunnlag(json: JsonNode): Beregningsgrunnlag = Beregningsgrunnlag(
        totalbeløp = json["totalbeløp"].asDouble(),
        omregnedeÅrsinntekter = omregnedeÅrsinntekter(json["omregnedeÅrsinntekter"])
    )

    private fun omregnedeÅrsinntekter(json: JsonNode): List<OmregnetÅrsinntekt> = json.map {
        OmregnetÅrsinntekt(
            arbeidsgiverreferanse = it["arbeidsgiverreferanse"].asText(),
            beløp = it["beløp"].asDouble()
        )
    }

    private fun sammenligningsgrunnlag(json: JsonNode): Sammenligningsgrunnlag = Sammenligningsgrunnlag(
        totalbeløp = json["totalbeløp"].asDouble(),
        innraporterteInntekter = innrapporterteInntekter(json["innraporterteInntekter"])
    )

    private fun innrapporterteInntekter(json: JsonNode): List<InnrapportertInntekt> = json.map {
        InnrapportertInntekt(
            arbeidsgiverreferanse = it["arbeidsgiverreferanse"].asText(),
            inntekter = inntekter(it["inntekter"])
        )
    }

    private fun inntekter(json: JsonNode): List<Inntekt> = json.map {
        Inntekt(
            årMåned = it["årMåned"].asYearMonth(),
            beløp = it["beløp"].asDouble()
        )
    }
}