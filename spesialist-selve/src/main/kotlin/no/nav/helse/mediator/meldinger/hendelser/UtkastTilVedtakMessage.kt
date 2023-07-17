package no.nav.helse.mediator.meldinger.hendelser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import java.util.UUID
import no.nav.helse.mediator.asUUID
import no.nav.helse.modell.vedtaksperiode.vedtak.Faktatype
import no.nav.helse.modell.vedtaksperiode.vedtak.Sykepengegrunnlagsfakta
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.isMissingOrNull

internal class UtkastTilVedtakMessage(packet: JsonMessage) {

    private val id = UUID.fromString(packet["@id"].asText())
    private val fødselsnummer = packet["fødselsnummer"].asText()
    private val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val utbetalingId = packet["utbetalingId"].takeUnless(JsonNode::isMissingOrNull)?.asUUID()
    private val skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate()
    private val hendelser = packet["hendelser"].map { it.asUUID() }
    private val sykepengegrunnlag = packet["sykepengegrunnlag"].asDouble()
    private val grunnlagForSykepengegrunnlag = packet["grunnlagForSykepengegrunnlag"].asDouble()
    private val grunnlagForSykepengegrunnlagPerArbeidsgiver = jacksonObjectMapper().treeToValue<Map<String, Double>>(packet["grunnlagForSykepengegrunnlagPerArbeidsgiver"])
    private val begrensning = packet["begrensning"].asText()
    private val inntekt = packet["inntekt"].asDouble()
    private val sykepengegrunnlagsfakta = utbetalingId?.let { sykepengegrunnlagsfakta(packet, faktatype(packet)) }

    private fun faktatype(packet: JsonMessage): Faktatype {
        return when (val fastsattString = packet["sykepengegrunnlagsfakta.fastsatt"].asText()) {
            "EtterSkjønn" -> Faktatype.ETTER_SKJØNN
            "EtterHovedregel" -> Faktatype.ETTER_HOVEDREGEL
            "IInfotrygd" -> Faktatype.I_INFOTRYGD
            else -> throw IllegalArgumentException("FastsattType $fastsattString er ikke støttet")
        }
    }

    private fun sykepengegrunnlagsfakta(packet: JsonMessage, faktatype: Faktatype): Sykepengegrunnlagsfakta {
        return when (faktatype) {
            Faktatype.ETTER_SKJØNN -> Sykepengegrunnlagsfakta.Spleis.EtterSkjønn(
                omregnetÅrsinntekt = packet["sykepengegrunnlagsfakta.omregnetÅrsinntekt"].asDouble(),
                innrapportertÅrsinntekt = packet["sykepengegrunnlagsfakta.innrapportertÅrsinntekt"].asDouble(),
                avviksprosent = packet["sykepengegrunnlagsfakta.avviksprosent"].asDouble(),
                seksG = packet["sykepengegrunnlagsfakta.6G"].asInt(),
                skjønnsfastsatt = packet["sykepengegrunnlagsfakta.skjønnsfastsatt"].asDouble(),
                tags = packet["sykepengegrunnlagsfakta.tags"].map { it.asText() },
                arbeidsgivere = packet["sykepengegrunnlagsfakta.arbeidsgivere"].map {
                    Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterSkjønn(
                        it["arbeidsgiver"].asText(),
                        it["omregnetÅrsinntekt"].asDouble(),
                        it["skjønnsfastsatt"].asDouble()
                    )
                },
            )

            Faktatype.ETTER_HOVEDREGEL -> Sykepengegrunnlagsfakta.Spleis.EtterHovedregel(
                omregnetÅrsinntekt = packet["sykepengegrunnlagsfakta.omregnetÅrsinntekt"].asDouble(),
                innrapportertÅrsinntekt = packet["sykepengegrunnlagsfakta.innrapportertÅrsinntekt"].asDouble(),
                avviksprosent = packet["sykepengegrunnlagsfakta.avviksprosent"].asDouble(),
                seksG = packet["sykepengegrunnlagsfakta.6G"].asInt(),
                tags = packet["sykepengegrunnlagsfakta.tags"].map { it.asText() },
                arbeidsgivere = packet["sykepengegrunnlagsfakta.arbeidsgivere"].map {
                    Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterHovedregel(
                        it["arbeidsgiver"].asText(),
                        it["omregnetÅrsinntekt"].asDouble(),
                    )
                },
            )

            Faktatype.I_INFOTRYGD -> Sykepengegrunnlagsfakta.Infotrygd(
                omregnetÅrsinntekt = packet["sykepengegrunnlagsfakta.omregnetÅrsinntekt"].asDouble(),
            )
        }
    }
}