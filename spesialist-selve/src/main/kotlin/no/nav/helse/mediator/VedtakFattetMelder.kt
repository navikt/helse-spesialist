package no.nav.helse.mediator

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.modell.person.PersonObserver
import no.nav.helse.modell.vedtaksperiode.IVedtaksperiodeObserver
import no.nav.helse.modell.vedtaksperiode.vedtak.Sykepengegrunnlagsfakta.Infotrygd
import no.nav.helse.modell.vedtaksperiode.vedtak.Sykepengegrunnlagsfakta.Spleis
import no.nav.helse.modell.vedtaksperiode.vedtak.Sykepengevedtak
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import org.slf4j.LoggerFactory

internal class VedtakFattetMelder(
    private val messageContext: MessageContext,
) : PersonObserver, IVedtaksperiodeObserver {
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
                is Sykepengevedtak.AuuVedtak -> auuVedtakJson(sykepengevedtak)
                is Sykepengevedtak.Vedtak -> vedtakJson(sykepengevedtak)
            }
        logg.info("Publiserer vedtak_fattet for {}", kv("vedtaksperiodeId", sykepengevedtak.vedtaksperiodeId))
        sikkerLogg.info(
            "Publiserer vedtak_fattet for {}, {}, {}",
            kv("fødselsnummer", sykepengevedtak.fødselsnummer),
            kv("organisasjonsnummer", sykepengevedtak.organisasjonsnummer),
            kv("vedtaksperiodeId", sykepengevedtak.vedtaksperiodeId),
        )
        messageContext.publish(sykepengevedtak.fødselsnummer, json)
        this.sykepengevedtak.clear()
    }

    override fun vedtakFattet(sykepengevedtak: Sykepengevedtak) {
        this.sykepengevedtak.add(sykepengevedtak)
    }

    private fun vedtakJson(sykepengevedtak: Sykepengevedtak.Vedtak): String {
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
                        when (sykepengevedtak.sykepengegrunnlagsfakta) {
                            is Spleis -> {
                                mutableMapOf(
                                    "omregnetÅrsinntekt" to sykepengevedtak.sykepengegrunnlagsfakta.omregnetÅrsinntekt,
                                    "innrapportertÅrsinntekt" to sykepengevedtak.sykepengegrunnlagsfakta.innrapportertÅrsinntekt,
                                    "avviksprosent" to sykepengevedtak.sykepengegrunnlagsfakta.avviksprosent,
                                    "6G" to sykepengevedtak.sykepengegrunnlagsfakta.seksG,
                                    "tags" to sykepengevedtak.sykepengegrunnlagsfakta.tags,
                                    "arbeidsgivere" to
                                        sykepengevedtak.sykepengegrunnlagsfakta.arbeidsgivere.map {
                                            mutableMapOf(
                                                "arbeidsgiver" to it.organisasjonsnummer,
                                                "omregnetÅrsinntekt" to it.omregnetÅrsinntekt,
                                                "innrapportertÅrsinntekt" to it.innrapportertÅrsinntekt,
                                            ).apply {
                                                if (it is Spleis.Arbeidsgiver.EtterSkjønn) {
                                                    put(
                                                        "skjønnsfastsatt",
                                                        it.skjønnsfastsatt,
                                                    )
                                                }
                                            }
                                        },
                                ).apply {
                                    when (sykepengevedtak.sykepengegrunnlagsfakta) {
                                        is Spleis.EtterHovedregel -> put("fastsatt", "EtterHovedregel")
                                        is Spleis.EtterSkjønn -> {
                                            put("fastsatt", "EtterSkjønn")
                                            put("skjønnsfastsettingtype", checkNotNull(sykepengevedtak.skjønnsfastsettingopplysninger?.skjønnsfastsettingtype))
                                            put("skjønnsfastsettingårsak", checkNotNull(sykepengevedtak.skjønnsfastsettingopplysninger?.skjønnsfastsettingsårsak))
                                            put(
                                                "skjønnsfastsatt",
                                                sykepengevedtak.sykepengegrunnlagsfakta.skjønnsfastsatt,
                                            )
                                        }
                                    }
                                }
                            }
                            is Infotrygd -> {
                                mapOf(
                                    "fastsatt" to "IInfotrygd",
                                    "omregnetÅrsinntekt" to sykepengevedtak.sykepengegrunnlagsfakta.omregnetÅrsinntekt,
                                )
                            }
                        },
                ).apply {
                    if (sykepengevedtak.sykepengegrunnlagsfakta is Spleis.EtterSkjønn) {
                        val skjønnsfastsettingopplysninger = sykepengevedtak.skjønnsfastsettingopplysninger!!
                        put(
                            "begrunnelser",
                            listOf(
                                mapOf(
                                    "type" to "SkjønnsfastsattSykepengegrunnlagMal",
                                    "begrunnelse" to skjønnsfastsettingopplysninger.begrunnelseFraMal,
                                    "perioder" to
                                        listOf(
                                            mapOf(
                                                "fom" to "${sykepengevedtak.fom}", "tom" to "${sykepengevedtak.tom}",
                                            ),
                                        ),
                                ),
                                mapOf(
                                    "type" to "SkjønnsfastsattSykepengegrunnlagFritekst",
                                    "begrunnelse" to skjønnsfastsettingopplysninger.begrunnelseFraFritekst,
                                    "perioder" to
                                        listOf(
                                            mapOf(
                                                "fom" to "${sykepengevedtak.fom}", "tom" to "${sykepengevedtak.tom}",
                                            ),
                                        ),
                                ),
                                mapOf(
                                    "type" to "SkjønnsfastsattSykepengegrunnlagKonklusjon",
                                    "begrunnelse" to skjønnsfastsettingopplysninger.begrunnelseFraKonklusjon,
                                    "perioder" to
                                        listOf(
                                            mapOf(
                                                "fom" to "${sykepengevedtak.fom}", "tom" to "${sykepengevedtak.tom}",
                                            ),
                                        ),
                                ),
                            ),
                        )
                    } else {
                        put("begrunnelser", emptyList<Map<String, Any>>())
                    }
                },
            )

        return message.toJson()
    }

    private fun auuVedtakJson(sykepengevedtak: Sykepengevedtak.AuuVedtak): String {
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
}
