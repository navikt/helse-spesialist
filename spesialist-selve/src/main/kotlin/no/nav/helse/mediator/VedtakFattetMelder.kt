package no.nav.helse.mediator

import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.modell.person.PersonObserver
import no.nav.helse.modell.vedtak.AvslagstypeDto
import no.nav.helse.modell.vedtak.Sykepengegrunnlagsfakta.Infotrygd
import no.nav.helse.modell.vedtak.Sykepengegrunnlagsfakta.Spleis
import no.nav.helse.modell.vedtak.Sykepengevedtak
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
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
                buildMap {
                    put("fødselsnummer", sykepengevedtak.fødselsnummer)
                    put("aktørId", sykepengevedtak.aktørId)
                    put("vedtaksperiodeId", "${sykepengevedtak.vedtaksperiodeId}")
                    put("behandlingId", "${sykepengevedtak.spleisBehandlingId}")
                    put("organisasjonsnummer", sykepengevedtak.organisasjonsnummer)
                    put("fom", "${sykepengevedtak.fom}")
                    put("tom", "${sykepengevedtak.tom}")
                    put("skjæringstidspunkt", "${sykepengevedtak.skjæringstidspunkt}")
                    put("hendelser", sykepengevedtak.hendelser)
                    put("sykepengegrunnlag", sykepengevedtak.sykepengegrunnlag)
                    put("grunnlagForSykepengegrunnlag", sykepengevedtak.grunnlagForSykepengegrunnlag)
                    put("grunnlagForSykepengegrunnlagPerArbeidsgiver", sykepengevedtak.grunnlagForSykepengegrunnlagPerArbeidsgiver)
                    put("begrensning", sykepengevedtak.begrensning)
                    put("inntekt", sykepengevedtak.inntekt)
                    put("vedtakFattetTidspunkt", "${sykepengevedtak.vedtakFattetTidspunkt}")
                    put("utbetalingId", "${sykepengevedtak.utbetalingId}")
                    put("tags", sykepengevedtak.tags)

                    sykepengegrunnlagsfakta(sykepengevedtak)

                    put("begrunnelser", byggBegrunnelser(sykepengevedtak))
                },
            )

        return message.toJson()
    }

    private fun MutableMap<String, Any>.sykepengegrunnlagsfakta(sykepengevedtak: Sykepengevedtak.Vedtak) {
        put(
            "sykepengegrunnlagsfakta",
            when (sykepengevedtak.sykepengegrunnlagsfakta) {
                is Spleis -> {
                    val sykepengegrunnlagsfakta = (sykepengevedtak.sykepengegrunnlagsfakta as Spleis)
                    buildMap {
                        put("omregnetÅrsinntekt", sykepengegrunnlagsfakta.omregnetÅrsinntekt)
                        put("innrapportertÅrsinntekt", sykepengegrunnlagsfakta.innrapportertÅrsinntekt)
                        put("avviksprosent", sykepengegrunnlagsfakta.avviksprosent)
                        put("6G", sykepengegrunnlagsfakta.seksG)
                        put("tags", sykepengegrunnlagsfakta.tags)
                        put(
                            "arbeidsgivere",
                            sykepengegrunnlagsfakta.arbeidsgivere.map {
                                buildMap {
                                    put("arbeidsgiver", it.organisasjonsnummer)
                                    put("omregnetÅrsinntekt", it.omregnetÅrsinntekt)
                                    put("innrapportertÅrsinntekt", it.innrapportertÅrsinntekt)
                                    if (it is Spleis.Arbeidsgiver.EtterSkjønn) put("skjønnsfastsatt", it.skjønnsfastsatt)
                                }
                            },
                        )
                        fastsetting(sykepengegrunnlagsfakta, sykepengevedtak)
                    }
                }

                is Infotrygd -> {
                    mapOf(
                        "fastsatt" to "IInfotrygd",
                        "omregnetÅrsinntekt" to sykepengevedtak.sykepengegrunnlagsfakta.omregnetÅrsinntekt,
                    )
                }
            },
        )
    }

    private fun MutableMap<String, Any>.fastsetting(
        sykepengegrunnlagsfakta: Spleis,
        sykepengevedtak: Sykepengevedtak.Vedtak,
    ) {
        when (sykepengegrunnlagsfakta) {
            is Spleis.EtterHovedregel -> put("fastsatt", "EtterHovedregel")
            is Spleis.EtterSkjønn -> {
                put("fastsatt", "EtterSkjønn")
                put(
                    "skjønnsfastsettingtype",
                    checkNotNull(sykepengevedtak.skjønnsfastsettingopplysninger?.skjønnsfastsettingtype),
                )
                put(
                    "skjønnsfastsettingårsak",
                    checkNotNull(sykepengevedtak.skjønnsfastsettingopplysninger?.skjønnsfastsettingsårsak),
                )
                put(
                    "skjønnsfastsatt",
                    (sykepengevedtak.sykepengegrunnlagsfakta as Spleis.EtterSkjønn).skjønnsfastsatt,
                )
            }
        }
    }

    private fun byggBegrunnelser(sykepengevedtak: Sykepengevedtak.Vedtak) =
        buildList {
            if (sykepengevedtak.sykepengegrunnlagsfakta is Spleis.EtterSkjønn) {
                addAll(byggSkjønnsmessigBegrunnelser(sykepengevedtak))
            }

            if (sykepengevedtak.avslag != null) {
                add(byggAvslagsbegrunnelse(sykepengevedtak))
            }
        }

    private fun byggSkjønnsmessigBegrunnelser(sykepengevedtak: Sykepengevedtak.Vedtak): List<Map<String, Any>> {
        val skjønnsfastsettingopplysninger = sykepengevedtak.skjønnsfastsettingopplysninger!!
        return listOf(
            mapOf(
                "type" to "SkjønnsfastsattSykepengegrunnlagMal",
                "begrunnelse" to skjønnsfastsettingopplysninger.begrunnelseFraMal,
                "perioder" to sykepengevedtak.lagPeriode(),
            ),
            mapOf(
                "type" to "SkjønnsfastsattSykepengegrunnlagFritekst",
                "begrunnelse" to skjønnsfastsettingopplysninger.begrunnelseFraFritekst,
                "perioder" to sykepengevedtak.lagPeriode(),
            ),
            mapOf(
                "type" to "SkjønnsfastsattSykepengegrunnlagKonklusjon",
                "begrunnelse" to skjønnsfastsettingopplysninger.begrunnelseFraKonklusjon,
                "perioder" to sykepengevedtak.lagPeriode(),
            ),
        )
    }

    private fun byggAvslagsbegrunnelse(sykepengevedtak: Sykepengevedtak.Vedtak) =
        mapOf(
            "type" to
                when (sykepengevedtak.avslag!!.type) {
                    AvslagstypeDto.AVSLAG -> "Avslag"
                    AvslagstypeDto.DELVIS_AVSLAG -> "DelvisInnvilgelse"
                },
            "begrunnelse" to sykepengevedtak.avslag!!.begrunnelse,
            "perioder" to sykepengevedtak.lagPeriode(),
        )

    private fun Sykepengevedtak.Vedtak.lagPeriode() = listOf(mapOf("fom" to "$fom", "tom" to "$tom"))

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
