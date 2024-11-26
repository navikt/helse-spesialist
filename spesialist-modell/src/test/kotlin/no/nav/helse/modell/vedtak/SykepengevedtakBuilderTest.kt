package no.nav.helse.modell.vedtak

import no.nav.helse.modell.januar
import no.nav.helse.modell.vedtak.Faktatype.ETTER_HOVEDREGEL
import no.nav.helse.modell.vedtak.Faktatype.ETTER_SKJØNN
import no.nav.helse.modell.vedtak.Faktatype.I_INFOTRYGD
import no.nav.helse.modell.vedtak.Sykepengegrunnlagsfakta.Infotrygd
import no.nav.helse.modell.vedtak.Sykepengegrunnlagsfakta.Spleis
import no.nav.helse.modell.vedtak.Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver
import no.nav.helse.modell.vedtak.VedtakBegrunnelseDto.UtfallDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID

class SykepengevedtakBuilderTest {
    private companion object {
        private const val fødselsnummer = "12345678910"
        private const val aktørId = "1234567891011"
        private const val organisasjonsnummer = "123456789"
        private val vedtaksperiodeId = UUID.randomUUID()
        private val spleisBehandlingId = UUID.randomUUID()
        private val utbetalingId = UUID.randomUUID()
        private val fom = 1.januar
        private val tom = 31.januar
        private val skjæringstidspunkt = 1.januar
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
            .sykepengegrunnlagsfakta(sykepengegrunnlagsfakta(ETTER_HOVEDREGEL))
            .vedtakBegrunnelse(VedtakBegrunnelse(Utfall.INNVILGELSE, null))
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
                sykepengegrunnlagsfakta = sykepengegrunnlagsfakta(ETTER_HOVEDREGEL),
                skjønnsfastsettingopplysninger = null,
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                tags = setOf("IngenNyArbeidsgiverperiode"),
                vedtakBegrunnelse = VedtakBegrunnelse(Utfall.INNVILGELSE, null).toDto(),
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
            .sykepengegrunnlagsfakta(sykepengegrunnlagsfakta(ETTER_SKJØNN))
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
                sykepengegrunnlagsfakta = sykepengegrunnlagsfakta(ETTER_SKJØNN),
                skjønnsfastsettingopplysninger =
                SkjønnsfastsettingopplysningerDto(
                    "Mal",
                    "Fritekst",
                    "Konklusjon",
                    Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT,
                    Skjønnsfastsettingsårsak.ANDRE_AVSNITT,
                ),
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                tags = setOf("IngenNyArbeidsgiverperiode"),
                vedtakBegrunnelse = VedtakBegrunnelse(Utfall.INNVILGELSE, null).toDto(),
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
            .sykepengegrunnlagsfakta(sykepengegrunnlagsfakta(ETTER_HOVEDREGEL))
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
                sykepengegrunnlagsfakta = sykepengegrunnlagsfakta(ETTER_HOVEDREGEL),
                skjønnsfastsettingopplysninger = null,
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                tags = setOf("IngenNyArbeidsgiverperiode"),
                vedtakBegrunnelse = VedtakBegrunnelseDto(
                    UtfallDto.DELVIS_INNVILGELSE,
                    "En individuell begrunnelse for avslag"
                ),
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
            .sykepengegrunnlagsfakta(sykepengegrunnlagsfakta(ETTER_SKJØNN))
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
                sykepengegrunnlagsfakta = sykepengegrunnlagsfakta(ETTER_SKJØNN),
                skjønnsfastsettingopplysninger =
                SkjønnsfastsettingopplysningerDto(
                    "Mal",
                    "Fritekst",
                    "Konklusjon",
                    Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT,
                    Skjønnsfastsettingsårsak.ANDRE_AVSNITT,
                ),
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                tags = setOf("IngenNyArbeidsgiverperiode"),
                vedtakBegrunnelse = VedtakBegrunnelseDto(
                    UtfallDto.AVSLAG,
                    "En individuell begrunnelse for avslag"
                ),
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
            .sykepengegrunnlagsfakta(sykepengegrunnlagsfakta(I_INFOTRYGD))
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
                sykepengegrunnlagsfakta = sykepengegrunnlagsfakta(I_INFOTRYGD),
                skjønnsfastsettingopplysninger = null,
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                tags = setOf("IngenNyArbeidsgiverperiode"),
                vedtakBegrunnelse = VedtakBegrunnelseDto(UtfallDto.INNVILGELSE, null),
            ),
            utkast,
        )
    }

    @Test
    fun `Bygg AUU-vedtak`() {
        val utkast = SykepengevedtakBuilder()
            .fødselsnummer(fødselsnummer)
            .aktørId(aktørId)
            .organisasjonsnummer(organisasjonsnummer)
            .vedtaksperiodeId(vedtaksperiodeId)
            .spleisBehandlingId(spleisBehandlingId)
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
            .tags(listOf("IngenNyArbeidsgiverperiode"))
            .build()

        assertTrue(utkast is Sykepengevedtak.AuuVedtak)
        assertEquals(
            Sykepengevedtak.AuuVedtak(
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
                organisasjonsnummer = organisasjonsnummer,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                hendelser = hendelser,
                sykepengegrunnlag = sykepengegrunnlag,
                grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag,
                grunnlagForSykepengegrunnlagPerArbeidsgiver = grunnlagForSykepengegrunnlagPerArbeidsgiver,
                begrensning = begrensning,
                inntekt = inntekt,
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                tags = setOf("IngenNyArbeidsgiverperiode"),
            ),
            utkast,
        )
    }

    @Test
    fun `Mangel på både utbetalingId og sykepengegrunnlagsfakta medfører AUU-vedtak`() {
        val builder = SykepengevedtakBuilder()
            .fødselsnummer(fødselsnummer)
            .aktørId(aktørId)
            .organisasjonsnummer(organisasjonsnummer)
            .vedtaksperiodeId(vedtaksperiodeId)
            .spleisBehandlingId(spleisBehandlingId)
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
            .tags(listOf("IngenNyArbeidsgiverperiode"))

        assertTrue(builder.build() is Sykepengevedtak.AuuVedtak)
    }

    @Test
    fun `Mangel på sykepengegrunnlagsfakta medfører bygging av AUU-vedtak`() {
        val builder = SykepengevedtakBuilder()
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
            .tags(listOf("IngenNyArbeidsgiverperiode"))

        assertTrue(builder.build() is Sykepengevedtak.AuuVedtak)
    }

    @Test
    fun `Mangel på utbetalingId medfører bygging av AUU-vedtak`() {
        val builder = SykepengevedtakBuilder()
            .fødselsnummer(fødselsnummer)
            .aktørId(aktørId)
            .organisasjonsnummer(organisasjonsnummer)
            .vedtaksperiodeId(vedtaksperiodeId)
            .spleisBehandlingId(spleisBehandlingId)
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
            .sykepengegrunnlagsfakta(sykepengegrunnlagsfakta(ETTER_HOVEDREGEL))
            .tags(listOf("IngenNyArbeidsgiverperiode"))

        assertTrue(builder.build() is Sykepengevedtak.AuuVedtak)
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
            .sykepengegrunnlagsfakta(sykepengegrunnlagsfakta(ETTER_SKJØNN))
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
            .sykepengegrunnlagsfakta(sykepengegrunnlagsfakta(ETTER_HOVEDREGEL))
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
                sykepengegrunnlagsfakta = sykepengegrunnlagsfakta(ETTER_HOVEDREGEL),
                skjønnsfastsettingopplysninger = null,
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                tags = setOf("IngenNyArbeidsgiverperiode"),
                vedtakBegrunnelse = VedtakBegrunnelseDto(UtfallDto.INNVILGELSE, null),
            ),
            utkast,
        )
    }

    private fun sykepengegrunnlagsfakta(faktatype: Faktatype): Sykepengegrunnlagsfakta {
        return when (faktatype) {
            ETTER_SKJØNN ->
                Spleis.EtterSkjønn(
                    omregnetÅrsinntekt = omregnetÅrsinntekt,
                    innrapportertÅrsinntekt = innrapportertÅrsinntekt,
                    avviksprosent = avviksprosent,
                    seksG = seksG2023,
                    skjønnsfastsatt = 650000.0,
                    tags = mutableSetOf(),
                    arbeidsgivere =
                    listOf(
                        Arbeidsgiver.EtterSkjønn(organisasjonsnummer, 300000.0, 300000.0, 325000.0),
                        Arbeidsgiver.EtterSkjønn("987654321", 300000.0, 300000.0, 325000.0),
                    ),
                )

            ETTER_HOVEDREGEL ->
                Spleis.EtterHovedregel(
                    omregnetÅrsinntekt = omregnetÅrsinntekt,
                    innrapportertÅrsinntekt = innrapportertÅrsinntekt,
                    avviksprosent = avviksprosent,
                    seksG = seksG2023,
                    tags = mutableSetOf(),
                    arbeidsgivere =
                    listOf(
                        Arbeidsgiver.EtterHovedregel(organisasjonsnummer, 300000.0, 300000.0),
                        Arbeidsgiver.EtterHovedregel("987654321", 300000.0, 300000.0),
                    ),
                )

            I_INFOTRYGD ->
                Infotrygd(
                    omregnetÅrsinntekt = omregnetÅrsinntekt,
                )
        }
    }
}
