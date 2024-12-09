package no.nav.helse.kafka.message_builders

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.helse.modell.vedtak.Sykepengevedtak
import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.modell.vedtak.VedtakBegrunnelse

internal fun Sykepengevedtak.somJsonMessage() =
    when (this) {
        is Sykepengevedtak.IkkeRealitetsbehandlet -> auuVedtakJson()
        is Sykepengevedtak.VedtakMedOpphavIInfotrygd -> vedtakMedOpphavIInfotrygdJson()
        is Sykepengevedtak.Vedtak -> vedtakJson()
        is Sykepengevedtak.VedtakMedSkjønnsvurdering -> vedtakMedSkjønnsvurderingJson()
    }

private fun Sykepengevedtak.Vedtak.vedtakJson(): String {
    val sykepengegrunnlagsfakta = sykepengegrunnlagsfakta
    val sammenligningsgrunnlag = sammenligningsgrunnlag

    val begrunnelser: List<Map<String, Any>> =
        emptyList<Map<String, Any>>()
            .supplerMedIndividuellBegrunnelse(vedtakBegrunnelse, this)

    val message =
        JsonMessage.newMessage(
            "vedtak_fattet",
            mutableMapOf(
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "behandlingId" to "$spleisBehandlingId",
                "organisasjonsnummer" to organisasjonsnummer,
                "fom" to "$fom",
                "tom" to "$tom",
                "skjæringstidspunkt" to "$skjæringstidspunkt",
                "hendelser" to hendelser,
                "sykepengegrunnlag" to sykepengegrunnlag,
                "grunnlagForSykepengegrunnlag" to grunnlagForSykepengegrunnlag,
                "grunnlagForSykepengegrunnlagPerArbeidsgiver" to grunnlagForSykepengegrunnlagPerArbeidsgiver,
                "begrensning" to begrensning,
                "inntekt" to inntekt,
                "vedtakFattetTidspunkt" to "$vedtakFattetTidspunkt",
                "utbetalingId" to "$utbetalingId",
                "tags" to tags,
                "sykepengegrunnlagsfakta" to
                    mutableMapOf(
                        "omregnetÅrsinntekt" to sykepengegrunnlagsfakta.omregnetÅrsinntekt,
                        "innrapportertÅrsinntekt" to sammenligningsgrunnlag.totalbeløp,
                        "avviksprosent" to avviksprosent,
                        "6G" to sykepengegrunnlagsfakta.seksG,
                        "tags" to sykepengegrunnlagsfakta.tags,
                        "arbeidsgivere" to
                            sykepengegrunnlagsfakta.arbeidsgivere.map {
                                mutableMapOf(
                                    "arbeidsgiver" to it.organisasjonsnummer,
                                    "omregnetÅrsinntekt" to it.omregnetÅrsinntekt,
                                    "innrapportertÅrsinntekt" to sammenligningsgrunnlag.innrapportertÅrsinntektFor(it.organisasjonsnummer),
                                )
                            },
                        "fastsatt" to "EtterHovedregel",
                    ),
                "begrunnelser" to begrunnelser,
            ),
        )

    return message.toJson()
}

private fun Sykepengevedtak.VedtakMedSkjønnsvurdering.vedtakMedSkjønnsvurderingJson(): String {
    val skjønnsfastsettingopplysninger = skjønnsfastsettingopplysninger
    val sykepengegrunnlagsfakta = sykepengegrunnlagsfakta
    val sammenligningsgrunnlag = sammenligningsgrunnlag
    val begrunnelser: List<Map<String, Any>> =
        emptyList<Map<String, Any>>()
            .supplerMedSkjønnsfastsettingsbegrunnelse(skjønnsfastsettingopplysninger, this)
            .supplerMedIndividuellBegrunnelse(vedtakBegrunnelse, this)
    val message =
        JsonMessage.newMessage(
            "vedtak_fattet",
            mutableMapOf(
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "behandlingId" to "$spleisBehandlingId",
                "organisasjonsnummer" to organisasjonsnummer,
                "fom" to "$fom",
                "tom" to "$tom",
                "skjæringstidspunkt" to "$skjæringstidspunkt",
                "hendelser" to hendelser,
                "sykepengegrunnlag" to sykepengegrunnlag,
                "grunnlagForSykepengegrunnlag" to grunnlagForSykepengegrunnlag,
                "grunnlagForSykepengegrunnlagPerArbeidsgiver" to grunnlagForSykepengegrunnlagPerArbeidsgiver,
                "begrensning" to begrensning,
                "inntekt" to inntekt,
                "vedtakFattetTidspunkt" to "$vedtakFattetTidspunkt",
                "utbetalingId" to "$utbetalingId",
                "tags" to tags,
                "sykepengegrunnlagsfakta" to
                    mutableMapOf(
                        "omregnetÅrsinntekt" to sykepengegrunnlagsfakta.omregnetÅrsinntekt,
                        "innrapportertÅrsinntekt" to sammenligningsgrunnlag.totalbeløp,
                        "avviksprosent" to avviksprosent,
                        "6G" to sykepengegrunnlagsfakta.seksG,
                        "tags" to sykepengegrunnlagsfakta.tags,
                        "arbeidsgivere" to
                            sykepengegrunnlagsfakta.arbeidsgivere.map {
                                mutableMapOf(
                                    "arbeidsgiver" to it.organisasjonsnummer,
                                    "omregnetÅrsinntekt" to it.omregnetÅrsinntekt,
                                    "innrapportertÅrsinntekt" to sammenligningsgrunnlag.innrapportertÅrsinntektFor(it.organisasjonsnummer),
                                    "skjønnsfastsatt" to it.skjønnsfastsatt,
                                )
                            },
                        "fastsatt" to "EtterSkjønn",
                        "skjønnsfastsettingtype" to skjønnsfastsettingopplysninger.skjønnsfastsettingtype,
                        "skjønnsfastsettingårsak" to skjønnsfastsettingopplysninger.skjønnsfastsettingsårsak,
                        "skjønnsfastsatt" to sykepengegrunnlagsfakta.skjønnsfastsatt,
                    ),
                "begrunnelser" to begrunnelser,
            ),
        )

    return message.toJson()
}

private fun Sykepengevedtak.VedtakMedOpphavIInfotrygd.vedtakMedOpphavIInfotrygdJson(): String {
    val begrunnelser: List<Map<String, Any>> =
        emptyList<Map<String, Any>>()
            .supplerMedIndividuellBegrunnelse(vedtakBegrunnelse, this)
    val message =
        JsonMessage.newMessage(
            "vedtak_fattet",
            mutableMapOf(
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "behandlingId" to "$spleisBehandlingId",
                "organisasjonsnummer" to organisasjonsnummer,
                "fom" to "$fom",
                "tom" to "$tom",
                "skjæringstidspunkt" to "$skjæringstidspunkt",
                "hendelser" to hendelser,
                "sykepengegrunnlag" to sykepengegrunnlag,
                "grunnlagForSykepengegrunnlag" to grunnlagForSykepengegrunnlag,
                "grunnlagForSykepengegrunnlagPerArbeidsgiver" to grunnlagForSykepengegrunnlagPerArbeidsgiver,
                "begrensning" to begrensning,
                "inntekt" to inntekt,
                "vedtakFattetTidspunkt" to "$vedtakFattetTidspunkt",
                "utbetalingId" to "$utbetalingId",
                "tags" to tags,
                "sykepengegrunnlagsfakta" to
                    mapOf(
                        "fastsatt" to "IInfotrygd",
                        "omregnetÅrsinntekt" to sykepengegrunnlagsfakta.omregnetÅrsinntekt,
                    ),
                "begrunnelser" to begrunnelser,
            ),
        )

    return message.toJson()
}

private fun Sykepengevedtak.IkkeRealitetsbehandlet.auuVedtakJson(): String {
    val message =
        JsonMessage.newMessage(
            "vedtak_fattet",
            mapOf(
                "fødselsnummer" to fødselsnummer,
                "aktørId" to aktørId,
                "vedtaksperiodeId" to "$vedtaksperiodeId",
                "behandlingId" to "$spleisBehandlingId",
                "organisasjonsnummer" to organisasjonsnummer,
                "fom" to "$fom",
                "tom" to "$tom",
                "skjæringstidspunkt" to "$skjæringstidspunkt",
                "hendelser" to hendelser,
                "sykepengegrunnlag" to sykepengegrunnlag,
                "grunnlagForSykepengegrunnlag" to grunnlagForSykepengegrunnlag,
                "grunnlagForSykepengegrunnlagPerArbeidsgiver" to grunnlagForSykepengegrunnlagPerArbeidsgiver,
                "begrensning" to begrensning,
                "inntekt" to inntekt,
                "vedtakFattetTidspunkt" to "$vedtakFattetTidspunkt",
                "begrunnelser" to emptyList<Map<String, Any>>(),
                "tags" to tags,
            ),
        )
    return message.toJson()
}

private fun List<Map<String, Any>>.supplerMedIndividuellBegrunnelse(
    vedtakBegrunnelse: VedtakBegrunnelse?,
    sykepengevedtak: Sykepengevedtak,
): List<Map<String, Any>> {
    if (vedtakBegrunnelse == null) return this
    return this +
        mapOf(
            "type" to
                when (vedtakBegrunnelse.utfall) {
                    Utfall.AVSLAG -> "Avslag"
                    Utfall.DELVIS_INNVILGELSE -> "DelvisInnvilgelse"
                    Utfall.INNVILGELSE -> "Innvilgelse"
                },
            "begrunnelse" to (vedtakBegrunnelse.begrunnelse ?: ""),
            "perioder" to
                listOf(
                    mapOf(
                        "fom" to "${sykepengevedtak.fom}",
                        "tom" to "${sykepengevedtak.tom}",
                    ),
                ),
        )
}

private fun List<Map<String, Any>>.supplerMedSkjønnsfastsettingsbegrunnelse(
    skjønnsfastsettingopplysninger: Sykepengevedtak.VedtakMedSkjønnsvurdering.Skjønnsfastsettingopplysninger,
    sykepengevedtak: Sykepengevedtak,
): List<Map<String, Any>> {
    return this +
        listOf(
            mapOf(
                "type" to "SkjønnsfastsattSykepengegrunnlagMal",
                "begrunnelse" to skjønnsfastsettingopplysninger.begrunnelseFraMal,
                "perioder" to
                    listOf(
                        mapOf(
                            "fom" to "${sykepengevedtak.fom}",
                            "tom" to "${sykepengevedtak.tom}",
                        ),
                    ),
            ),
            mapOf(
                "type" to "SkjønnsfastsattSykepengegrunnlagFritekst",
                "begrunnelse" to skjønnsfastsettingopplysninger.begrunnelseFraFritekst,
                "perioder" to
                    listOf(
                        mapOf(
                            "fom" to "${sykepengevedtak.fom}",
                            "tom" to "${sykepengevedtak.tom}",
                        ),
                    ),
            ),
            mapOf(
                "type" to "SkjønnsfastsattSykepengegrunnlagKonklusjon",
                "begrunnelse" to skjønnsfastsettingopplysninger.begrunnelseFraKonklusjon,
                "perioder" to
                    listOf(
                        mapOf(
                            "fom" to "${sykepengevedtak.fom}",
                            "tom" to "${sykepengevedtak.tom}",
                        ),
                    ),
            ),
        )
}
