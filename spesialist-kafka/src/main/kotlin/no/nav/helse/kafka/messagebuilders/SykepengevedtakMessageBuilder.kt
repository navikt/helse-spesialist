package no.nav.helse.kafka.messagebuilders

import no.nav.helse.modell.melding.VedtakFattetMelding
import java.time.LocalDate

internal fun VedtakFattetMelding.detaljer(): Map<String, Any> =
    mapOf(
        "fødselsnummer" to fødselsnummer,
        "aktørId" to aktørId,
        "yrkesaktivitetstype" to yrkesaktivitetstype,
        "vedtaksperiodeId" to vedtaksperiodeId,
        "behandlingId" to behandlingId,
        "organisasjonsnummer" to organisasjonsnummer,
        "fom" to fom,
        "tom" to tom,
        "skjæringstidspunkt" to skjæringstidspunkt,
        "hendelser" to hendelser,
        "sykepengegrunnlag" to sykepengegrunnlag,
        "vedtakFattetTidspunkt" to vedtakFattetTidspunkt.toString(),
        "utbetalingId" to utbetalingId,
        "tags" to tags,
        "sykepengegrunnlagsfakta" to sykepengegrunnlagsfakta.tilSykepengegrunnlagsfakta(),
        "begrunnelser" to begrunnelser.map { it.tilBegrunnelse(fom = fom, tom = tom) },
    )

private fun VedtakFattetMelding.Sykepengegrunnlagsfakta.tilSykepengegrunnlagsfakta(): Map<String, Any> =
    when (this) {
        is VedtakFattetMelding.FastsattEtterHovedregelSykepengegrunnlagsfakta -> tilSykepengegrunnlagsfakta()
        is VedtakFattetMelding.FastsattEtterSkjønnSykepengegrunnlagsfakta -> tilSykepengegrunnlagsfakta()
        is VedtakFattetMelding.FastsattIInfotrygdSykepengegrunnlagsfakta -> tilSykepengegrunnlagsfakta()
        is VedtakFattetMelding.SelvstendigNæringsdrivendeSykepengegrunnlagsfakta -> tilSykepengegrunnlagsfakta()
    }

private fun VedtakFattetMelding.FastsattEtterHovedregelSykepengegrunnlagsfakta.tilSykepengegrunnlagsfakta(): Map<String, Any> =
    mapOf(
        "omregnetÅrsinntekt" to omregnetÅrsinntekt,
        "innrapportertÅrsinntekt" to innrapportertÅrsinntekt,
        "avviksprosent" to avviksprosent,
        "6G" to seksG,
        "tags" to tags,
        "arbeidsgivere" to
                arbeidsgivere.map {
                    mapOf(
                        "arbeidsgiver" to it.organisasjonsnummer,
                        "omregnetÅrsinntekt" to it.omregnetÅrsinntekt,
                        "innrapportertÅrsinntekt" to it.innrapportertÅrsinntekt,
                    )
                },
        "fastsatt" to "EtterHovedregel",
    )

private fun VedtakFattetMelding.FastsattEtterSkjønnSykepengegrunnlagsfakta.tilSykepengegrunnlagsfakta(): Map<String, Any> =
    mapOf(
        "omregnetÅrsinntekt" to omregnetÅrsinntekt,
        "innrapportertÅrsinntekt" to innrapportertÅrsinntekt,
        "avviksprosent" to avviksprosent,
        "6G" to seksG,
        "tags" to tags,
        "arbeidsgivere" to
                arbeidsgivere.map {
                    mapOf(
                        "arbeidsgiver" to it.organisasjonsnummer,
                        "omregnetÅrsinntekt" to it.omregnetÅrsinntekt,
                        "innrapportertÅrsinntekt" to it.innrapportertÅrsinntekt,
                        "skjønnsfastsatt" to it.skjønnsfastsatt,
                    )
                },
        "fastsatt" to "EtterSkjønn",
        "skjønnsfastsettingtype" to skjønnsfastsettingtype,
        "skjønnsfastsettingårsak" to skjønnsfastsettingsårsak,
        "skjønnsfastsatt" to skjønnsfastsatt,
    )

private fun VedtakFattetMelding.FastsattIInfotrygdSykepengegrunnlagsfakta.tilSykepengegrunnlagsfakta(): Map<String, Any> =
    mapOf(
        "fastsatt" to "IInfotrygd",
        "omregnetÅrsinntekt" to omregnetÅrsinntekt,
    )

private fun VedtakFattetMelding.SelvstendigNæringsdrivendeSykepengegrunnlagsfakta.tilSykepengegrunnlagsfakta(): Map<String, Any> =
    mapOf(
        "fastsatt" to "EtterHovedregel",
        "6G" to seksG,
        "tags" to tags,
        "selvstendig" to
                mapOf(
                    "beregningsgrunnlag" to beregningsgrunnlag,
                    "pensjonsgivendeInntekter" to pensjonsgivendeInntekter.map { inntekt ->
                        mapOf(
                            "årstall" to inntekt.årstall,
                            "beløp" to inntekt.beløp
                        )
                    }
                ),
    )

private fun VedtakFattetMelding.Begrunnelse.tilBegrunnelse(
    fom: LocalDate,
    tom: LocalDate,
): Map<String, Any> =
    mapOf(
        "type" to type,
        "begrunnelse" to begrunnelse,
        "perioder" to listOf(mapOf("fom" to fom, "tom" to tom)),
    )
