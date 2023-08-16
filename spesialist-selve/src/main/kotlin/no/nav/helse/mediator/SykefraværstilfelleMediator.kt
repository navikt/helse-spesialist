package no.nav.helse.mediator

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.modell.sykefraværstilfelle.SykefraværstilfelleObserver
import no.nav.helse.modell.vedtaksperiode.vedtak.Sykepengegrunnlagsfakta.Infotrygd
import no.nav.helse.modell.vedtaksperiode.vedtak.Sykepengegrunnlagsfakta.Spleis
import no.nav.helse.modell.vedtaksperiode.vedtak.Sykepengevedtak
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

internal class SykefraværstilfelleMediator(
    private val rapidsConnection: RapidsConnection,
) : SykefraværstilfelleObserver {
    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val logg = LoggerFactory.getLogger(SykefraværstilfelleMediator::class.java)
    }

    override fun vedtakFattet(sykepengevedtak: Sykepengevedtak) {
        val json = when (sykepengevedtak) {
            is Sykepengevedtak.AuuVedtak -> auuVedtakJson(sykepengevedtak)
            is Sykepengevedtak.Vedtak -> vedtakJson(sykepengevedtak)
        }
        logg.info("Publiserer vedtak_fattet for {}", kv("vedtaksperiodeId", sykepengevedtak.vedtaksperiodeId))
        sikkerLogg.info(
            "Publiserer vedtak_fattet for {}, {}, {}",
            kv("fødselsnummer", sykepengevedtak.fødselsnummer),
            kv("organisasjonsnummer", sykepengevedtak.organisasjonsnummer),
            kv("vedtaksperiodeId", sykepengevedtak.vedtaksperiodeId)
        )
        rapidsConnection.publish(sykepengevedtak.fødselsnummer, json)
    }

    private fun vedtakJson(sykepengevedtak: Sykepengevedtak.Vedtak): String {
        val message = JsonMessage.newMessage("vedtak_fattet", mutableMapOf(
            "fødselsnummer" to sykepengevedtak.fødselsnummer,
            "aktørId" to sykepengevedtak.aktørId,
            "vedtaksperiodeId" to "${sykepengevedtak.vedtaksperiodeId}",
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
            "sykepengegrunnlagsfakta" to
                    when (sykepengevedtak.sykepengegrunnlagsfakta) {
                        is Spleis -> {
                            mutableMapOf(
                                "omregnetÅrsinntekt" to sykepengevedtak.sykepengegrunnlagsfakta.omregnetÅrsinntekt,
                                "innrapportertÅrsinntekt" to sykepengevedtak.sykepengegrunnlagsfakta.innrapportertÅrsinntekt,
                                "avviksprosent" to sykepengevedtak.sykepengegrunnlagsfakta.avviksprosent,
                                "6G" to sykepengevedtak.sykepengegrunnlagsfakta.seksG,
                                "tags" to sykepengevedtak.sykepengegrunnlagsfakta.tags,
                                "arbeidsgivere" to sykepengevedtak.sykepengegrunnlagsfakta.arbeidsgivere.map {
                                    mutableMapOf(
                                        "arbeidsgiver" to it.organisasjonsnummer,
                                        "omregnetÅrsinntekt" to it.omregnetArsinntekt,
                                    ).apply {
                                        if (it is Spleis.Arbeidsgiver.EtterSkjønn) put(
                                            "skjønnsfastsatt",
                                            it.skjønnsfastsatt
                                        )
                                    }
                                }
                            ).apply {
                                when (sykepengevedtak.sykepengegrunnlagsfakta) {
                                    is Spleis.EtterHovedregel -> put("fastsatt", "EtterHovedregel")
                                    is Spleis.EtterSkjønn -> {
                                        put("fastsatt", "EtterSkjønn")
                                        put(
                                            "skjønnsfastsatt",
                                            sykepengevedtak.sykepengegrunnlagsfakta.skjønnsfastsatt
                                        )
                                    }
                                }
                            }
                        }
                        is Infotrygd -> {
                            mapOf(
                                "fastsatt" to "IInfotrygd",
                                "omregnetÅrsinntekt" to sykepengevedtak.sykepengegrunnlagsfakta.omregnetÅrsinntekt
                            )
                        }
                    }
        ).apply {
            if (sykepengevedtak.sykepengegrunnlagsfakta is Spleis.EtterSkjønn) put(
                "begrunnelser", listOf(
                    mapOf(
                        "type" to "SkjønnsfastsattSykepengegrunnlagMal",
                        "begrunnelse" to sykepengevedtak.begrunnelseFraMal,
                        "perioder" to listOf(
                            mapOf(
                                "fom" to "${sykepengevedtak.fom}", "tom" to "${sykepengevedtak.tom}"
                            )
                        )
                    ),
                    mapOf(
                        "type" to "SkjønnsfastsattSykepengegrunnlagFritekst",
                        "begrunnelse" to sykepengevedtak.begrunnelseFraFritekst,
                        "perioder" to listOf(
                            mapOf(
                                "fom" to "${sykepengevedtak.fom}", "tom" to "${sykepengevedtak.tom}"
                            )
                        )
                    )
                )
            )
            else put("begrunnelser", emptyList<Map<String, Any>>())
        }
        )

        return message.toJson()
    }

    private fun auuVedtakJson(sykepengevedtak: Sykepengevedtak.AuuVedtak): String {
        val message = JsonMessage.newMessage(
            "vedtak_fattet", mapOf(
                "fødselsnummer" to sykepengevedtak.fødselsnummer,
                "aktørId" to sykepengevedtak.aktørId,
                "vedtaksperiodeId" to "${sykepengevedtak.vedtaksperiodeId}",
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
                "begrunnelser" to emptyList<Map<String, Any>>()
            )
        )
        return message.toJson()
    }
}