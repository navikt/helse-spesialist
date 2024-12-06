package no.nav.helse.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.modell.person.PersonObserver
import no.nav.helse.modell.vedtak.Sykepengevedtak
import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.modell.vedtak.VedtakBegrunnelse
import org.slf4j.LoggerFactory

internal class VedtakFattetMelder(
    private val messageContext: MessageContext,
) : PersonObserver {
    private val sykepengevedtak = mutableListOf<Sykepengevedtak>()

    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val logg = LoggerFactory.getLogger(VedtakFattetMelder::class.java)
    }

    internal fun publiserUtgåendeMeldinger() {
        if (sykepengevedtak.isEmpty()) return
        check(sykepengevedtak.size == 1) { "Forventer å publisere kun ett vedtak" }
        val sykepengevedtak = sykepengevedtak.single()
        val json =
            when (sykepengevedtak) {
                is Sykepengevedtak.IkkeRealitetsbehandlet -> auuVedtakJson(sykepengevedtak)
                is Sykepengevedtak.Vedtak -> vedtakJson(sykepengevedtak)
                is Sykepengevedtak.VedtakMedOpphavIInfotrygd -> vedtakMedOpphavIInfotrygdJson(sykepengevedtak)
                is Sykepengevedtak.VedtakMedSkjønnsvurdering -> vedtakMedSkjønnsvurderingJson(sykepengevedtak)
            }
        logg.info("Publiserer vedtak_fattet for {}", kv("vedtaksperiodeId", sykepengevedtak.vedtaksperiodeId))
        sikkerLogg.info(
            "Publiserer vedtak_fattet for {}, {}, {}",
            kv("fødselsnummer", sykepengevedtak.fødselsnummer),
            kv("organisasjonsnummer", sykepengevedtak.organisasjonsnummer),
            kv("vedtaksperiodeId", sykepengevedtak.vedtaksperiodeId),
        )
        messageContext.publish(json)
        this.sykepengevedtak.clear()
    }

    override fun vedtakFattet(sykepengevedtak: Sykepengevedtak) {
        this.sykepengevedtak.add(sykepengevedtak)
    }

    private fun vedtakJson(sykepengevedtak: Sykepengevedtak.Vedtak): String {
        val begrunnelser: List<Map<String, Any>> =
            emptyList<Map<String, Any>>()
                .supplerMedIndividuellBegrunnelse(sykepengevedtak.vedtakBegrunnelse, sykepengevedtak)
        val sykepengegrunnlagsfakta = sykepengevedtak.sykepengegrunnlagsfakta
        val message =
            JsonMessage.newMessage(
                "vedtak_fattet",
                mutableMapOf(
                    "fødselsnummer" to sykepengevedtak.fødselsnummer,
                    "aktørId" to sykepengevedtak.aktørId,
                    "vedtaksperiodeId" to "${sykepengevedtak.vedtaksperiodeId}",
                    "behandlingId" to "${sykepengevedtak.spleisBehandlingId}",
                    "organisasjonsnummer" to sykepengevedtak.organisasjonsnummer,
                    "fom" to "${sykepengevedtak.fom}",
                    "tom" to "${sykepengevedtak.tom}",
                    "skjæringstidspunkt" to "${sykepengevedtak.skjæringstidspunkt}",
                    "hendelser" to sykepengevedtak.hendelser,
                    "sykepengegrunnlag" to sykepengevedtak.sykepengegrunnlag,
                    "grunnlagForSykepengegrunnlag" to sykepengevedtak.grunnlagForSykepengegrunnlag,
                    "grunnlagForSykepengegrunnlagPerArbeidsgiver" to sykepengevedtak.grunnlagForSykepengegrunnlagPerArbeidsgiver,
                    "begrensning" to sykepengevedtak.begrensning,
                    "inntekt" to sykepengevedtak.inntekt,
                    "vedtakFattetTidspunkt" to "${sykepengevedtak.vedtakFattetTidspunkt}",
                    "utbetalingId" to "${sykepengevedtak.utbetalingId}",
                    "tags" to sykepengevedtak.tags,
                    "sykepengegrunnlagsfakta" to
                        mutableMapOf(
                            "omregnetÅrsinntekt" to sykepengegrunnlagsfakta.omregnetÅrsinntekt,
                            "innrapportertÅrsinntekt" to sykepengegrunnlagsfakta.innrapportertÅrsinntekt,
                            "avviksprosent" to sykepengegrunnlagsfakta.avviksprosent,
                            "6G" to sykepengegrunnlagsfakta.seksG,
                            "tags" to sykepengegrunnlagsfakta.tags,
                            "arbeidsgivere" to
                                sykepengegrunnlagsfakta.arbeidsgivere.map {
                                    mutableMapOf(
                                        "arbeidsgiver" to it.organisasjonsnummer,
                                        "omregnetÅrsinntekt" to it.omregnetÅrsinntekt,
                                        "innrapportertÅrsinntekt" to it.innrapportertÅrsinntekt,
                                    )
                                },
                            "fastsatt" to "EtterHovedregel",
                        ),
                    "begrunnelser" to begrunnelser,
                ),
            )

        return message.toJson()
    }

    private fun vedtakMedSkjønnsvurderingJson(sykepengevedtak: Sykepengevedtak.VedtakMedSkjønnsvurdering): String {
        val begrunnelser: List<Map<String, Any>> =
            emptyList<Map<String, Any>>()
                .supplerMedSkjønnsfastsettingsbegrunnelse(sykepengevedtak.skjønnsfastsettingopplysninger, sykepengevedtak)
                .supplerMedIndividuellBegrunnelse(sykepengevedtak.vedtakBegrunnelse, sykepengevedtak)
        val sykepengegrunnlagsfakta = sykepengevedtak.sykepengegrunnlagsfakta
        val message =
            JsonMessage.newMessage(
                "vedtak_fattet",
                mutableMapOf(
                    "fødselsnummer" to sykepengevedtak.fødselsnummer,
                    "aktørId" to sykepengevedtak.aktørId,
                    "vedtaksperiodeId" to "${sykepengevedtak.vedtaksperiodeId}",
                    "behandlingId" to "${sykepengevedtak.spleisBehandlingId}",
                    "organisasjonsnummer" to sykepengevedtak.organisasjonsnummer,
                    "fom" to "${sykepengevedtak.fom}",
                    "tom" to "${sykepengevedtak.tom}",
                    "skjæringstidspunkt" to "${sykepengevedtak.skjæringstidspunkt}",
                    "hendelser" to sykepengevedtak.hendelser,
                    "sykepengegrunnlag" to sykepengevedtak.sykepengegrunnlag,
                    "grunnlagForSykepengegrunnlag" to sykepengevedtak.grunnlagForSykepengegrunnlag,
                    "grunnlagForSykepengegrunnlagPerArbeidsgiver" to sykepengevedtak.grunnlagForSykepengegrunnlagPerArbeidsgiver,
                    "begrensning" to sykepengevedtak.begrensning,
                    "inntekt" to sykepengevedtak.inntekt,
                    "vedtakFattetTidspunkt" to "${sykepengevedtak.vedtakFattetTidspunkt}",
                    "utbetalingId" to "${sykepengevedtak.utbetalingId}",
                    "tags" to sykepengevedtak.tags,
                    "sykepengegrunnlagsfakta" to
                        mutableMapOf(
                            "omregnetÅrsinntekt" to sykepengegrunnlagsfakta.omregnetÅrsinntekt,
                            "innrapportertÅrsinntekt" to sykepengegrunnlagsfakta.innrapportertÅrsinntekt,
                            "avviksprosent" to sykepengegrunnlagsfakta.avviksprosent,
                            "6G" to sykepengegrunnlagsfakta.seksG,
                            "tags" to sykepengegrunnlagsfakta.tags,
                            "arbeidsgivere" to
                                sykepengegrunnlagsfakta.arbeidsgivere.map {
                                    mutableMapOf(
                                        "arbeidsgiver" to it.organisasjonsnummer,
                                        "omregnetÅrsinntekt" to it.omregnetÅrsinntekt,
                                        "innrapportertÅrsinntekt" to it.innrapportertÅrsinntekt,
                                        "skjønnsfastsatt" to it.skjønnsfastsatt,
                                    )
                                },
                            "fastsatt" to "EtterSkjønn",
                            "skjønnsfastsettingtype" to sykepengevedtak.skjønnsfastsettingopplysninger.skjønnsfastsettingtype,
                            "skjønnsfastsettingårsak" to sykepengevedtak.skjønnsfastsettingopplysninger.skjønnsfastsettingsårsak,
                            "skjønnsfastsatt" to sykepengevedtak.sykepengegrunnlagsfakta.skjønnsfastsatt,
                        ),
                    "begrunnelser" to begrunnelser,
                ),
            )

        return message.toJson()
    }

    private fun vedtakMedOpphavIInfotrygdJson(sykepengevedtak: Sykepengevedtak.VedtakMedOpphavIInfotrygd): String {
        val begrunnelser: List<Map<String, Any>> =
            emptyList<Map<String, Any>>()
                .supplerMedIndividuellBegrunnelse(sykepengevedtak.vedtakBegrunnelse, sykepengevedtak)
        val message =
            JsonMessage.newMessage(
                "vedtak_fattet",
                mutableMapOf(
                    "fødselsnummer" to sykepengevedtak.fødselsnummer,
                    "aktørId" to sykepengevedtak.aktørId,
                    "vedtaksperiodeId" to "${sykepengevedtak.vedtaksperiodeId}",
                    "behandlingId" to "${sykepengevedtak.spleisBehandlingId}",
                    "organisasjonsnummer" to sykepengevedtak.organisasjonsnummer,
                    "fom" to "${sykepengevedtak.fom}",
                    "tom" to "${sykepengevedtak.tom}",
                    "skjæringstidspunkt" to "${sykepengevedtak.skjæringstidspunkt}",
                    "hendelser" to sykepengevedtak.hendelser,
                    "sykepengegrunnlag" to sykepengevedtak.sykepengegrunnlag,
                    "grunnlagForSykepengegrunnlag" to sykepengevedtak.grunnlagForSykepengegrunnlag,
                    "grunnlagForSykepengegrunnlagPerArbeidsgiver" to sykepengevedtak.grunnlagForSykepengegrunnlagPerArbeidsgiver,
                    "begrensning" to sykepengevedtak.begrensning,
                    "inntekt" to sykepengevedtak.inntekt,
                    "vedtakFattetTidspunkt" to "${sykepengevedtak.vedtakFattetTidspunkt}",
                    "utbetalingId" to "${sykepengevedtak.utbetalingId}",
                    "tags" to sykepengevedtak.tags,
                    "sykepengegrunnlagsfakta" to
                        mapOf(
                            "fastsatt" to "IInfotrygd",
                            "omregnetÅrsinntekt" to sykepengevedtak.sykepengegrunnlagsfakta.omregnetÅrsinntekt,
                        ),
                    "begrunnelser" to begrunnelser,
                ),
            )

        return message.toJson()
    }

    private fun auuVedtakJson(sykepengevedtak: Sykepengevedtak.IkkeRealitetsbehandlet): String {
        val message =
            JsonMessage.newMessage(
                "vedtak_fattet",
                mapOf(
                    "fødselsnummer" to sykepengevedtak.fødselsnummer,
                    "aktørId" to sykepengevedtak.aktørId,
                    "vedtaksperiodeId" to "${sykepengevedtak.vedtaksperiodeId}",
                    "behandlingId" to "${sykepengevedtak.spleisBehandlingId}",
                    "organisasjonsnummer" to sykepengevedtak.organisasjonsnummer,
                    "fom" to "${sykepengevedtak.fom}",
                    "tom" to "${sykepengevedtak.tom}",
                    "skjæringstidspunkt" to "${sykepengevedtak.skjæringstidspunkt}",
                    "hendelser" to sykepengevedtak.hendelser,
                    "sykepengegrunnlag" to sykepengevedtak.sykepengegrunnlag,
                    "grunnlagForSykepengegrunnlag" to sykepengevedtak.grunnlagForSykepengegrunnlag,
                    "grunnlagForSykepengegrunnlagPerArbeidsgiver" to sykepengevedtak.grunnlagForSykepengegrunnlagPerArbeidsgiver,
                    "begrensning" to sykepengevedtak.begrensning,
                    "inntekt" to sykepengevedtak.inntekt,
                    "vedtakFattetTidspunkt" to "${sykepengevedtak.vedtakFattetTidspunkt}",
                    "begrunnelser" to emptyList<Map<String, Any>>(),
                    "tags" to sykepengevedtak.tags,
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
}
