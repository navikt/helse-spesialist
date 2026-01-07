package no.nav.helse.kafka.messagebuilders

import no.nav.helse.modell.melding.VedtakFattetMelding
import java.time.LocalDate

private fun normaliserNavn(navn: String): String {
    val deler = navn.split(",", limit = 2)
    if (deler.size != 2) return navn

    val etternavn = deler[0].trim()
    val fornavnOgMellomnavn = deler[1].trim()

    return "$fornavnOgMellomnavn $etternavn"
}

internal fun VedtakFattetMelding.detaljer(): Map<String, Any> =
    buildMap {
        put("fødselsnummer", fødselsnummer)
        put("aktørId", aktørId)
        put("yrkesaktivitetstype", yrkesaktivitetstype)
        put("vedtaksperiodeId", vedtaksperiodeId)
        put("behandlingId", behandlingId)
        put("organisasjonsnummer", organisasjonsnummer)
        put("fom", fom)
        put("tom", tom)
        put("skjæringstidspunkt", skjæringstidspunkt)
        put("hendelser", hendelser)
        put("sykepengegrunnlag", sykepengegrunnlag)
        put("vedtakFattetTidspunkt", vedtakFattetTidspunkt.toString())
        put("utbetalingId", utbetalingId)
        put("tags", tags)
        put("sykepengegrunnlagsfakta", sykepengegrunnlagsfakta.tilSykepengegrunnlagsfakta())
        put("begrunnelser", begrunnelser.map { it.tilBegrunnelse(fom = fom, tom = tom) })
        saksbehandler?.let { put("saksbehandler", mapOf("ident" to it.ident, "navn" to normaliserNavn(it.navn))) }
        beslutter?.let { put("beslutter", mapOf("ident" to it.ident, "navn" to normaliserNavn(it.navn))) }
        put("automatiskFattet", automatiskFattet)
        dekning?.let {
            put(
                "dekning",
                mapOf("dekningsgrad" to it.dekningsgrad, "gjelderFraDag" to it.gjelderFraDag),
            )
        }
    }

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
                "pensjonsgivendeInntekter" to
                    pensjonsgivendeInntekter.map { inntekt ->
                        mapOf(
                            "årstall" to inntekt.årstall,
                            "beløp" to inntekt.beløp,
                        )
                    },
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
