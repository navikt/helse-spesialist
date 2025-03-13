package no.nav.helse.modell.vedtak

import no.nav.helse.modell.melding.Sykepengevedtak
import no.nav.helse.modell.melding.Sykepengevedtak.VedtakMedSkjønnsvurdering
import no.nav.helse.modell.vedtak.Sykepengegrunnlagsfakta.Infotrygd
import no.nav.helse.modell.vedtak.Sykepengegrunnlagsfakta.Spleis
import no.nav.helse.modell.vilkårsprøving.InnrapportertInntekt
import no.nav.helse.modell.vilkårsprøving.Inntekt
import no.nav.helse.modell.vilkårsprøving.Sammenligningsgrunnlag
import no.nav.helse.spesialist.testhjelp.jan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class SykepengevedtakBuilderTest {
    private companion object {
        private const val fødselsnummer = "12345678910"
        private const val aktørId = "1234567891011"
        private const val organisasjonsnummer = "123456789"
        private val vedtaksperiodeId = UUID.randomUUID()
        private val spleisBehandlingId = UUID.randomUUID()
        private val utbetalingId = UUID.randomUUID()
        private val fom = 1 jan 2018
        private val tom = 31 jan 2018
        private val skjæringstidspunkt = 1 jan 2018
        private val hendelser = listOf(UUID.randomUUID(), UUID.randomUUID())
        private const val sykepengegrunnlag = 600000.00
        private const val grunnlagForSykepengegrunnlag = 600000.00
        private val grunnlagForSykepengegrunnlagPerArbeidsgiver =
            mapOf(
                organisasjonsnummer to 300000.00,
                "987654321" to 300000.00,
            )
        private const val begrensning = "ER_6G_BEGRENSET"
        private const val inntekt = 25000.00
        private val vedtakFattetTidspunkt = LocalDateTime.now()
        private const val omregnetÅrsinntekt = 300000.00
        private const val innrapportertÅrsinntekt = 300000.00
        private const val avviksprosent = 0.0
        private const val seksG2023 = 6 * 118620.0
    }

    @Test
    fun `Bygg vanlig vedtak - sykepengegrunnlag fastsatt etter hovedregel`() {
        val utkast = SykepengevedtakBuilder()
            .fødselsnummer(fødselsnummer)
            .aktørId(aktørId)
            .organisasjonsnummer(organisasjonsnummer)
            .vedtaksperiodeId(vedtaksperiodeId)
            .spleisBehandlingId(spleisBehandlingId)
            .utbetalingId(utbetalingId)
            .fom(fom)
            .tom(tom)
            .skjæringstidspunkt(skjæringstidspunkt)
            .hendelser(hendelser)
            .sykepengegrunnlag(sykepengegrunnlag)
            .grunnlagForSykepengegrunnlag(grunnlagForSykepengegrunnlag)
            .grunnlagForSykepengegrunnlagPerArbeidsgiver(grunnlagForSykepengegrunnlagPerArbeidsgiver)
            .begrensning(begrensning)
            .inntekt(inntekt)
            .vedtakFattetTidspunkt(vedtakFattetTidspunkt)
            .sykepengegrunnlagsfakta(sykepengegrunnlagsfakta<Spleis.EtterHovedregel>())
            .vedtakBegrunnelse(VedtakBegrunnelse(Utfall.INNVILGELSE, null))
            .avviksprosent(avviksprosent)
            .sammenligningsgrunnlag(sammenligningsgrunnlag(innrapportertÅrsinntekt, organisasjonsnummer))
            .tags(listOf("IngenNyArbeidsgiverperiode"))
            .build()

        assertTrue(utkast is Sykepengevedtak.Vedtak)
        assertEquals(
            Sykepengevedtak.Vedtak(
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
                utbetalingId = utbetalingId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                hendelser = hendelser,
                sykepengegrunnlag = sykepengegrunnlag,
                grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag,
                grunnlagForSykepengegrunnlagPerArbeidsgiver = grunnlagForSykepengegrunnlagPerArbeidsgiver,
                begrensning = begrensning,
                inntekt = inntekt,
                sykepengegrunnlagsfakta = sykepengegrunnlagsfakta<Spleis.EtterHovedregel>(),
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                tags = setOf("IngenNyArbeidsgiverperiode"),
                vedtakBegrunnelse = VedtakBegrunnelse(Utfall.INNVILGELSE, null),
                avviksprosent = avviksprosent,
                sammenligningsgrunnlag = sammenligningsgrunnlag(innrapportertÅrsinntekt, organisasjonsnummer),
            ),
            utkast,
        )
    }

    @Test
    fun `Bygg vanlig vedtak - sykepengegrunnlag fastsatt skjønn`() {
        val utkast = SykepengevedtakBuilder()
            .fødselsnummer(fødselsnummer)
            .aktørId(aktørId)
            .organisasjonsnummer(organisasjonsnummer)
            .vedtaksperiodeId(vedtaksperiodeId)
            .spleisBehandlingId(spleisBehandlingId)
            .utbetalingId(utbetalingId)
            .fom(fom)
            .tom(tom)
            .skjæringstidspunkt(skjæringstidspunkt)
            .hendelser(hendelser)
            .sykepengegrunnlag(sykepengegrunnlag)
            .grunnlagForSykepengegrunnlag(grunnlagForSykepengegrunnlag)
            .grunnlagForSykepengegrunnlagPerArbeidsgiver(grunnlagForSykepengegrunnlagPerArbeidsgiver)
            .begrensning(begrensning)
            .inntekt(inntekt)
            .vedtakFattetTidspunkt(vedtakFattetTidspunkt)
            .avviksprosent(avviksprosent)
            .sammenligningsgrunnlag(sammenligningsgrunnlag(innrapportertÅrsinntekt, organisasjonsnummer))
            .sykepengegrunnlagsfakta(sykepengegrunnlagsfakta<Spleis.EtterSkjønn>())
            .skjønnsfastsattSykepengegrunnlag(
                SkjønnsfastsattSykepengegrunnlag(
                    Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT,
                    Skjønnsfastsettingsårsak.ANDRE_AVSNITT,
                    skjæringstidspunkt,
                    "Mal",
                    "Fritekst",
                    "Konklusjon",
                    LocalDateTime.now(),
                ),
            )
            .tags(listOf("IngenNyArbeidsgiverperiode"))
            .vedtakBegrunnelse(VedtakBegrunnelse(Utfall.INNVILGELSE, null))
            .build()

        assertTrue(utkast is VedtakMedSkjønnsvurdering)
        assertEquals(
            VedtakMedSkjønnsvurdering(
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
                utbetalingId = utbetalingId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                hendelser = hendelser,
                sykepengegrunnlag = sykepengegrunnlag,
                grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag,
                grunnlagForSykepengegrunnlagPerArbeidsgiver = grunnlagForSykepengegrunnlagPerArbeidsgiver,
                begrensning = begrensning,
                inntekt = inntekt,
                sykepengegrunnlagsfakta = sykepengegrunnlagsfakta<Spleis.EtterSkjønn>(),
                skjønnsfastsettingopplysninger =
                    VedtakMedSkjønnsvurdering.Skjønnsfastsettingopplysninger(
                        "Mal",
                        "Fritekst",
                        "Konklusjon",
                        Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT,
                        Skjønnsfastsettingsårsak.ANDRE_AVSNITT,
                    ),
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                tags = setOf("IngenNyArbeidsgiverperiode"),
                vedtakBegrunnelse = VedtakBegrunnelse(Utfall.INNVILGELSE, null),
                avviksprosent = avviksprosent,
                sammenligningsgrunnlag = sammenligningsgrunnlag(innrapportertÅrsinntekt, organisasjonsnummer),
            ),
            utkast,
        )
    }

    @Test
    fun `Bygg vanlig vedtak - med delvis avslag`() {
        val utkast = SykepengevedtakBuilder()
            .fødselsnummer(fødselsnummer)
            .aktørId(aktørId)
            .organisasjonsnummer(organisasjonsnummer)
            .vedtaksperiodeId(vedtaksperiodeId)
            .spleisBehandlingId(spleisBehandlingId)
            .utbetalingId(utbetalingId)
            .fom(fom)
            .tom(tom)
            .skjæringstidspunkt(skjæringstidspunkt)
            .hendelser(hendelser)
            .sykepengegrunnlag(sykepengegrunnlag)
            .grunnlagForSykepengegrunnlag(grunnlagForSykepengegrunnlag)
            .grunnlagForSykepengegrunnlagPerArbeidsgiver(grunnlagForSykepengegrunnlagPerArbeidsgiver)
            .begrensning(begrensning)
            .inntekt(inntekt)
            .vedtakFattetTidspunkt(vedtakFattetTidspunkt)
            .avviksprosent(avviksprosent)
            .sammenligningsgrunnlag(sammenligningsgrunnlag(innrapportertÅrsinntekt, organisasjonsnummer))
            .sykepengegrunnlagsfakta(sykepengegrunnlagsfakta<Spleis.EtterHovedregel>())
            .vedtakBegrunnelse(
                VedtakBegrunnelse(Utfall.DELVIS_INNVILGELSE, "En individuell begrunnelse for avslag")
            )
            .tags(listOf("IngenNyArbeidsgiverperiode"))
            .build()

        assertTrue(utkast is Sykepengevedtak.Vedtak)
        assertEquals(
            Sykepengevedtak.Vedtak(
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
                utbetalingId = utbetalingId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                hendelser = hendelser,
                sykepengegrunnlag = sykepengegrunnlag,
                grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag,
                grunnlagForSykepengegrunnlagPerArbeidsgiver = grunnlagForSykepengegrunnlagPerArbeidsgiver,
                begrensning = begrensning,
                inntekt = inntekt,
                sykepengegrunnlagsfakta = sykepengegrunnlagsfakta<Spleis.EtterHovedregel>(),
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                tags = setOf("IngenNyArbeidsgiverperiode"),
                vedtakBegrunnelse = VedtakBegrunnelse(
                    Utfall.DELVIS_INNVILGELSE,
                    "En individuell begrunnelse for avslag"
                ),
                avviksprosent = avviksprosent,
                sammenligningsgrunnlag = sammenligningsgrunnlag(innrapportertÅrsinntekt, organisasjonsnummer),
            ),
            utkast,
        )
    }

    @Test
    fun `Bygg vanlig vedtak - sykepengegrunnlag fastsatt skjønn med avslag`() {
        val utkast = SykepengevedtakBuilder()
            .fødselsnummer(fødselsnummer)
            .aktørId(aktørId)
            .organisasjonsnummer(organisasjonsnummer)
            .vedtaksperiodeId(vedtaksperiodeId)
            .spleisBehandlingId(spleisBehandlingId)
            .utbetalingId(utbetalingId)
            .fom(fom)
            .tom(tom)
            .skjæringstidspunkt(skjæringstidspunkt)
            .hendelser(hendelser)
            .sykepengegrunnlag(sykepengegrunnlag)
            .grunnlagForSykepengegrunnlag(grunnlagForSykepengegrunnlag)
            .grunnlagForSykepengegrunnlagPerArbeidsgiver(grunnlagForSykepengegrunnlagPerArbeidsgiver)
            .begrensning(begrensning)
            .inntekt(inntekt)
            .vedtakFattetTidspunkt(vedtakFattetTidspunkt)
            .avviksprosent(avviksprosent)
            .sammenligningsgrunnlag(sammenligningsgrunnlag(innrapportertÅrsinntekt, organisasjonsnummer))
            .sykepengegrunnlagsfakta(sykepengegrunnlagsfakta<Spleis.EtterSkjønn>())
            .skjønnsfastsattSykepengegrunnlag(
                SkjønnsfastsattSykepengegrunnlag(
                    Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT,
                    Skjønnsfastsettingsårsak.ANDRE_AVSNITT,
                    skjæringstidspunkt,
                    "Mal",
                    "Fritekst",
                    "Konklusjon",
                    LocalDateTime.now(),
                ),
            )
            .vedtakBegrunnelse(
                VedtakBegrunnelse(Utfall.AVSLAG, "En individuell begrunnelse for avslag")
            )
            .tags(listOf("IngenNyArbeidsgiverperiode"))
            .build()

        assertTrue(utkast is VedtakMedSkjønnsvurdering)
        assertEquals(
            VedtakMedSkjønnsvurdering(
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
                utbetalingId = utbetalingId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                hendelser = hendelser,
                sykepengegrunnlag = sykepengegrunnlag,
                grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag,
                grunnlagForSykepengegrunnlagPerArbeidsgiver = grunnlagForSykepengegrunnlagPerArbeidsgiver,
                begrensning = begrensning,
                inntekt = inntekt,
                sykepengegrunnlagsfakta = sykepengegrunnlagsfakta<Spleis.EtterSkjønn>(),
                skjønnsfastsettingopplysninger = VedtakMedSkjønnsvurdering.Skjønnsfastsettingopplysninger(
                    "Mal",
                    "Fritekst",
                    "Konklusjon",
                    Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT,
                    Skjønnsfastsettingsårsak.ANDRE_AVSNITT,
                ),
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                tags = setOf("IngenNyArbeidsgiverperiode"),
                vedtakBegrunnelse = VedtakBegrunnelse(
                    Utfall.AVSLAG,
                    "En individuell begrunnelse for avslag"
                ),
                avviksprosent = avviksprosent,
                sammenligningsgrunnlag = sammenligningsgrunnlag(innrapportertÅrsinntekt, organisasjonsnummer),
            ),
            utkast,
        )
    }

    @Test
    fun `Bygg vanlig vedtak - sykepengegrunnlag fastsatt i Infotrygd`() {
        val utkast = SykepengevedtakBuilder()
            .fødselsnummer(fødselsnummer)
            .aktørId(aktørId)
            .organisasjonsnummer(organisasjonsnummer)
            .vedtaksperiodeId(vedtaksperiodeId)
            .spleisBehandlingId(spleisBehandlingId)
            .utbetalingId(utbetalingId)
            .fom(fom)
            .tom(tom)
            .skjæringstidspunkt(skjæringstidspunkt)
            .hendelser(hendelser)
            .sykepengegrunnlag(sykepengegrunnlag)
            .grunnlagForSykepengegrunnlag(grunnlagForSykepengegrunnlag)
            .grunnlagForSykepengegrunnlagPerArbeidsgiver(grunnlagForSykepengegrunnlagPerArbeidsgiver)
            .begrensning(begrensning)
            .inntekt(inntekt)
            .vedtakFattetTidspunkt(vedtakFattetTidspunkt)
            .sykepengegrunnlagsfakta(sykepengegrunnlagsfakta<Infotrygd>())
            .tags(listOf("IngenNyArbeidsgiverperiode"))
            .vedtakBegrunnelse(VedtakBegrunnelse(Utfall.INNVILGELSE, null))
            .build()

        assertTrue(utkast is Sykepengevedtak.VedtakMedOpphavIInfotrygd)
        assertEquals(
            Sykepengevedtak.VedtakMedOpphavIInfotrygd(
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
                utbetalingId = utbetalingId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                hendelser = hendelser,
                sykepengegrunnlag = sykepengegrunnlag,
                grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag,
                grunnlagForSykepengegrunnlagPerArbeidsgiver = grunnlagForSykepengegrunnlagPerArbeidsgiver,
                begrensning = begrensning,
                inntekt = inntekt,
                sykepengegrunnlagsfakta = sykepengegrunnlagsfakta<Infotrygd>(),
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                tags = setOf("IngenNyArbeidsgiverperiode"),
                vedtakBegrunnelse = VedtakBegrunnelse(Utfall.INNVILGELSE, null),
            ),
            utkast,
        )
    }

    @Test
    fun `Forventer at skjønnsfastsettingData er satt ved bygging av vedtak etter skjønn`() {
        val builder = SykepengevedtakBuilder()
            .fødselsnummer(fødselsnummer)
            .aktørId(aktørId)
            .organisasjonsnummer(organisasjonsnummer)
            .vedtaksperiodeId(vedtaksperiodeId)
            .utbetalingId(utbetalingId)
            .fom(fom)
            .tom(tom)
            .skjæringstidspunkt(skjæringstidspunkt)
            .hendelser(hendelser)
            .sykepengegrunnlag(sykepengegrunnlag)
            .grunnlagForSykepengegrunnlag(grunnlagForSykepengegrunnlag)
            .grunnlagForSykepengegrunnlagPerArbeidsgiver(grunnlagForSykepengegrunnlagPerArbeidsgiver)
            .begrensning(begrensning)
            .inntekt(inntekt)
            .vedtakFattetTidspunkt(vedtakFattetTidspunkt)
            .avviksprosent(avviksprosent)
            .sammenligningsgrunnlag(sammenligningsgrunnlag(innrapportertÅrsinntekt, organisasjonsnummer))
            .sykepengegrunnlagsfakta(sykepengegrunnlagsfakta<Spleis.EtterSkjønn>())
            .tags(listOf("IngenNyArbeidsgiverperiode"))

        assertThrows<IllegalStateException> { builder.build() }
    }

    @Test
    fun `Benytter ikke opplysninger fra skjønnsfastsetting selv om det er satt, ved bygging etter hovedregel`() {
        val utkast = SykepengevedtakBuilder()
            .fødselsnummer(fødselsnummer)
            .aktørId(aktørId)
            .organisasjonsnummer(organisasjonsnummer)
            .vedtaksperiodeId(vedtaksperiodeId)
            .spleisBehandlingId(spleisBehandlingId)
            .utbetalingId(utbetalingId)
            .fom(fom)
            .tom(tom)
            .skjæringstidspunkt(skjæringstidspunkt)
            .hendelser(hendelser)
            .sykepengegrunnlag(sykepengegrunnlag)
            .grunnlagForSykepengegrunnlag(grunnlagForSykepengegrunnlag)
            .grunnlagForSykepengegrunnlagPerArbeidsgiver(grunnlagForSykepengegrunnlagPerArbeidsgiver)
            .begrensning(begrensning)
            .inntekt(inntekt)
            .vedtakFattetTidspunkt(vedtakFattetTidspunkt)
            .sykepengegrunnlagsfakta(sykepengegrunnlagsfakta<Spleis.EtterHovedregel>())
            .avviksprosent(avviksprosent)
            .sammenligningsgrunnlag(sammenligningsgrunnlag(innrapportertÅrsinntekt, organisasjonsnummer))
            .skjønnsfastsettingData(
                "Fritekst",
                "Mal",
                "Konklusjon",
                Skjønnsfastsettingstype.RAPPORTERT_ÅRSINNTEKT,
                Skjønnsfastsettingsårsak.ANDRE_AVSNITT,
            )
            .tags(listOf("IngenNyArbeidsgiverperiode"))
            .vedtakBegrunnelse(VedtakBegrunnelse(Utfall.INNVILGELSE, null))
            .build()

        assertTrue(utkast is Sykepengevedtak.Vedtak)
        assertEquals(
            Sykepengevedtak.Vedtak(
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
                utbetalingId = utbetalingId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                hendelser = hendelser,
                sykepengegrunnlag = sykepengegrunnlag,
                grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag,
                grunnlagForSykepengegrunnlagPerArbeidsgiver = grunnlagForSykepengegrunnlagPerArbeidsgiver,
                begrensning = begrensning,
                inntekt = inntekt,
                sykepengegrunnlagsfakta = sykepengegrunnlagsfakta<Spleis.EtterHovedregel>(),
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                tags = setOf("IngenNyArbeidsgiverperiode"),
                vedtakBegrunnelse = VedtakBegrunnelse(Utfall.INNVILGELSE, null),
                avviksprosent = avviksprosent,
                sammenligningsgrunnlag = sammenligningsgrunnlag(innrapportertÅrsinntekt, organisasjonsnummer),
            ),
            utkast,
        )
    }

    private fun sammenligningsgrunnlag(
        totalbeløp: Double,
        vararg arbeidsgivere: String
    ): Sammenligningsgrunnlag {
        val start = YearMonth.from(skjæringstidspunkt.minusMonths(1))
        val end = YearMonth.from(skjæringstidspunkt.minusMonths(13))
        val måneder = generateSequence(start) {
            if (it > end) it.minusMonths(1) else null
        }.toList()
        return Sammenligningsgrunnlag(
            totalbeløp = totalbeløp,
            innrapporterteInntekter = arbeidsgivere.map { arbeidsgiverreferanse ->
                val arbeidsgiverandel = totalbeløp / arbeidsgivere.size
                val inntekter = måneder.map {
                    Inntekt(it, arbeidsgiverandel/måneder.size)
                }
                InnrapportertInntekt(arbeidsgiverreferanse, inntekter)
            }
        )
    }

    private inline fun <reified T: Sykepengegrunnlagsfakta> sykepengegrunnlagsfakta(): T {
        return when (T::class) {
            Spleis.EtterSkjønn::class ->
                Spleis.EtterSkjønn(
                    omregnetÅrsinntekt = omregnetÅrsinntekt,
                    seksG = seksG2023,
                    skjønnsfastsatt = 650000.0,
                    tags = mutableSetOf(),
                    arbeidsgivere =
                    listOf(
                        Spleis.Arbeidsgiver.EtterSkjønn(organisasjonsnummer, 300000.0, 325000.0),
                        Spleis.Arbeidsgiver.EtterSkjønn("987654321", 300000.0, 325000.0),
                    ),
                )

            Spleis.EtterHovedregel::class ->
                Spleis.EtterHovedregel(
                    omregnetÅrsinntekt = omregnetÅrsinntekt,
                    seksG = seksG2023,
                    tags = mutableSetOf(),
                    arbeidsgivere =
                    listOf(
                        Spleis.Arbeidsgiver.EtterHovedregel(organisasjonsnummer, 300000.0),
                        Spleis.Arbeidsgiver.EtterHovedregel("987654321", 300000.0),
                    ),
                )

            Infotrygd::class ->
                Infotrygd(
                    omregnetÅrsinntekt = omregnetÅrsinntekt,
                )

            else -> throw IllegalArgumentException("Støtter ikke type ${T::class.simpleName}")
        } as T
    }
}
