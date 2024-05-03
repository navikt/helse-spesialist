package no.nav.helse.mediator

import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.helse.TestRapidHelpers.meldinger
import no.nav.helse.januar
import no.nav.helse.modell.vedtak.AvslagDto
import no.nav.helse.modell.vedtak.Avslagstype
import no.nav.helse.modell.vedtak.SkjønnsfastsettingopplysningerDto
import no.nav.helse.modell.vedtak.Skjønnsfastsettingstype
import no.nav.helse.modell.vedtak.Skjønnsfastsettingsårsak
import no.nav.helse.modell.vedtak.Sykepengegrunnlagsfakta
import no.nav.helse.modell.vedtak.Sykepengevedtak
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class VedtakFattetMelderTest {
    private val testRapid = TestRapid()
    private val vedtakFattetMelder = VedtakFattetMelder(testRapid)

    private companion object {
        private val fødselsnummer = "12345678910"
        private val vedtaksperiodeId = UUID.randomUUID()
        private val spleisBehandlingId = UUID.randomUUID()
        private val utbetalingId = UUID.randomUUID()
        private val organisasjonsnummer = "123456789"
        private val aktørId = "1234567891011"
        private val fom = 1.januar
        private val tom = 31.januar
        private val skjæringstidspunkt = 1.januar
        private val hendelser = listOf(UUID.randomUUID())
        private val vedtakFattetTidspunkt = LocalDateTime.now()
    }

    @Test
    fun auuVedtak() {
        val auuVedtak =
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
                sykepengegrunnlag = 0.0,
                grunnlagForSykepengegrunnlag = 0.0,
                grunnlagForSykepengegrunnlagPerArbeidsgiver = emptyMap(),
                begrensning = "VET_IKKE",
                inntekt = 0.0,
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                tags = setOf("IngenNyArbeidsgiverperiode"),
            )
        vedtakFattetMelder.vedtakFattet(auuVedtak)
        vedtakFattetMelder.publiserUtgåendeMeldinger()
        val eventer = testRapid.inspektør.meldinger()

        assertEquals(1, eventer.size)

        val event = eventer.first()

        assertEquals("vedtak_fattet", event["@event_name"].asText())
        assertEquals(fødselsnummer, event["fødselsnummer"].asText())
        assertEquals(aktørId, event["aktørId"].asText())
        assertEquals(vedtaksperiodeId.toString(), event["vedtaksperiodeId"].asText())
        assertEquals(organisasjonsnummer, event["organisasjonsnummer"].asText())
        assertEquals(fom, event["fom"].asLocalDate())
        assertEquals(tom, event["tom"].asLocalDate())
        assertEquals(skjæringstidspunkt, event["skjæringstidspunkt"].asLocalDate())
        assertEquals(hendelser, event["hendelser"].map { UUID.fromString(it.asText()) })
        assertEquals(0.0, event["sykepengegrunnlag"].asDouble())
        assertEquals(0.0, event["grunnlagForSykepengegrunnlag"].asDouble())
        assertEquals(
            emptyMap<String, Double>(),
            objectMapper.convertValue<Map<String, Double>>(event["grunnlagForSykepengegrunnlagPerArbeidsgiver"]),
        )
        assertEquals("VET_IKKE", event["begrensning"].asText())
        assertEquals(0.0, event["inntekt"].asDouble())
        assertEquals(vedtakFattetTidspunkt, event["vedtakFattetTidspunkt"].asLocalDateTime())
        assertEquals(0, event["begrunnelser"].size())
        assertEquals(1, event["tags"].size())
        assertEquals("IngenNyArbeidsgiverperiode", event["tags"].first().asText())
    }

    @Test
    fun `vanlig vedtak sykepengegrunnlag fastsatt i infotrygd`() {
        val infotrygd =
            Sykepengevedtak.Vedtak(
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                vedtaksperiodeId = vedtaksperiodeId,
                organisasjonsnummer = organisasjonsnummer,
                spleisBehandlingId = spleisBehandlingId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                hendelser = hendelser,
                sykepengegrunnlag = 10000.0,
                grunnlagForSykepengegrunnlag = 10000.0,
                grunnlagForSykepengegrunnlagPerArbeidsgiver = mapOf(organisasjonsnummer to 10000.0),
                begrensning = "VURDERT_I_INFOTRYGD",
                inntekt = 10000.0,
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                utbetalingId = utbetalingId,
                sykepengegrunnlagsfakta =
                    Sykepengegrunnlagsfakta.Infotrygd(
                        omregnetÅrsinntekt = 10000.0,
                    ),
                skjønnsfastsettingopplysninger = null,
                tags = setOf("IngenNyArbeidsgiverperiode"),
            avslag = null,
        )
        vedtakFattetMelder.vedtakFattet(infotrygd)
        vedtakFattetMelder.publiserUtgåendeMeldinger()
        val eventer = testRapid.inspektør.meldinger()

        assertEquals(1, eventer.size)

        val event = eventer.first()

        assertEquals("vedtak_fattet", event["@event_name"].asText())
        assertEquals(fødselsnummer, event["fødselsnummer"].asText())
        assertEquals(aktørId, event["aktørId"].asText())
        assertEquals(vedtaksperiodeId.toString(), event["vedtaksperiodeId"].asText())
        assertEquals(organisasjonsnummer, event["organisasjonsnummer"].asText())
        assertEquals(fom, event["fom"].asLocalDate())
        assertEquals(tom, event["tom"].asLocalDate())
        assertEquals(skjæringstidspunkt, event["skjæringstidspunkt"].asLocalDate())
        assertEquals(hendelser, event["hendelser"].map { UUID.fromString(it.asText()) })
        assertEquals(10000.0, event["sykepengegrunnlag"].asDouble())
        assertEquals(10000.0, event["grunnlagForSykepengegrunnlag"].asDouble())
        assertEquals(
            mapOf(organisasjonsnummer to 10000.0),
            objectMapper.convertValue(event["grunnlagForSykepengegrunnlagPerArbeidsgiver"]),
        )
        assertEquals("VURDERT_I_INFOTRYGD", event["begrensning"].asText())
        assertEquals(10000.0, event["inntekt"].asDouble())
        assertEquals(vedtakFattetTidspunkt, event["vedtakFattetTidspunkt"].asLocalDateTime())
        assertEquals(utbetalingId.toString(), event["utbetalingId"].asText())
        assertEquals("IInfotrygd", event["sykepengegrunnlagsfakta"]["fastsatt"].asText())
        assertEquals(10000.0, event["sykepengegrunnlagsfakta"]["omregnetÅrsinntekt"].asDouble())
        assertEquals(0, event["begrunnelser"].size())
        assertEquals(1, event["tags"].size())
        assertEquals("IngenNyArbeidsgiverperiode", event["tags"].first().asText())
    }

    @Test
    fun `vanlig vedtak sykepengegrunnlag fastsatt etter hovedregel`() {
        val infotrygd =
            Sykepengevedtak.Vedtak(
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                vedtaksperiodeId = vedtaksperiodeId,
                organisasjonsnummer = organisasjonsnummer,
                spleisBehandlingId = spleisBehandlingId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                hendelser = hendelser,
                sykepengegrunnlag = 10000.0,
                grunnlagForSykepengegrunnlag = 10000.0,
                grunnlagForSykepengegrunnlagPerArbeidsgiver = mapOf(organisasjonsnummer to 10000.0),
                begrensning = "ER_IKKE_6G_BEGRENSET",
                inntekt = 10000.0,
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                utbetalingId = utbetalingId,
                sykepengegrunnlagsfakta =
                    Sykepengegrunnlagsfakta.Spleis.EtterHovedregel(
                        omregnetÅrsinntekt = 10000.0,
                        innrapportertÅrsinntekt = 10000.0,
                        avviksprosent = 0.0,
                        seksG = 711720.0,
                        tags = mutableSetOf(),
                        arbeidsgivere =
                            listOf(
                                Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterHovedregel(
                                    organisasjonsnummer = organisasjonsnummer,
                                    omregnetÅrsinntekt = 10000.0,
                                    innrapportertÅrsinntekt = 10000.0,
                                ),
                            ),
                    ),
                skjønnsfastsettingopplysninger = null,
                tags = setOf("IngenNyArbeidsgiverperiode"),
            avslag = null,
        )
        vedtakFattetMelder.vedtakFattet(infotrygd)
        vedtakFattetMelder.publiserUtgåendeMeldinger()
        val eventer = testRapid.inspektør.meldinger()

        assertEquals(1, eventer.size)

        val event = eventer.first()

        assertEquals("vedtak_fattet", event["@event_name"].asText())
        assertEquals(fødselsnummer, event["fødselsnummer"].asText())
        assertEquals(aktørId, event["aktørId"].asText())
        assertEquals(vedtaksperiodeId.toString(), event["vedtaksperiodeId"].asText())
        assertEquals(organisasjonsnummer, event["organisasjonsnummer"].asText())
        assertEquals(fom, event["fom"].asLocalDate())
        assertEquals(tom, event["tom"].asLocalDate())
        assertEquals(skjæringstidspunkt, event["skjæringstidspunkt"].asLocalDate())
        assertEquals(hendelser, event["hendelser"].map { UUID.fromString(it.asText()) })
        assertEquals(10000.0, event["sykepengegrunnlag"].asDouble())
        assertEquals(10000.0, event["grunnlagForSykepengegrunnlag"].asDouble())
        assertEquals(
            mapOf(organisasjonsnummer to 10000.0),
            objectMapper.convertValue(event["grunnlagForSykepengegrunnlagPerArbeidsgiver"]),
        )
        assertEquals("ER_IKKE_6G_BEGRENSET", event["begrensning"].asText())
        assertEquals(10000.0, event["inntekt"].asDouble())
        assertEquals(vedtakFattetTidspunkt, event["vedtakFattetTidspunkt"].asLocalDateTime())
        assertEquals(utbetalingId.toString(), event["utbetalingId"].asText())
        assertEquals("EtterHovedregel", event["sykepengegrunnlagsfakta"]["fastsatt"].asText())
        assertEquals(10000.0, event["sykepengegrunnlagsfakta"]["omregnetÅrsinntekt"].asDouble())
        assertEquals(10000.0, event["sykepengegrunnlagsfakta"]["innrapportertÅrsinntekt"].asDouble())
        assertEquals(0.0, event["sykepengegrunnlagsfakta"]["avviksprosent"].asDouble())
        assertEquals(711720.0, event["sykepengegrunnlagsfakta"]["6G"].asDouble())
        assertEquals(emptyList<String>(), objectMapper.convertValue(event["sykepengegrunnlagsfakta"]["tags"]))
        assertEquals(
            listOf(
                mapOf(
                    "arbeidsgiver" to organisasjonsnummer,
                    "omregnetÅrsinntekt" to 10000.0,
                    "innrapportertÅrsinntekt" to 10000.0,
                ),
            ),
            objectMapper.convertValue(event["sykepengegrunnlagsfakta"]["arbeidsgivere"]),
        )
        assertEquals(0, event["begrunnelser"].size())
        assertEquals(1, event["tags"].size())
        assertEquals("IngenNyArbeidsgiverperiode", event["tags"].first().asText())
    }

    @Test
    fun `vanlig vedtak sykepengegrunnlag fastsatt etter hovedregel med delvis avslag`() {
        val spleis =
            Sykepengevedtak.Vedtak(
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                vedtaksperiodeId = vedtaksperiodeId,
                organisasjonsnummer = organisasjonsnummer,
                spleisBehandlingId = spleisBehandlingId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                hendelser = hendelser,
                sykepengegrunnlag = 10000.0,
                grunnlagForSykepengegrunnlag = 10000.0,
                grunnlagForSykepengegrunnlagPerArbeidsgiver = mapOf(organisasjonsnummer to 10000.0),
                begrensning = "ER_IKKE_6G_BEGRENSET",
                inntekt = 10000.0,
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                utbetalingId = utbetalingId,
                sykepengegrunnlagsfakta =
                    Sykepengegrunnlagsfakta.Spleis.EtterHovedregel(
                        omregnetÅrsinntekt = 10000.0,
                        innrapportertÅrsinntekt = 10000.0,
                        avviksprosent = 0.0,
                        seksG = 711720.0,
                        tags = mutableSetOf(),
                        arbeidsgivere =
                            listOf(
                                Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterHovedregel(
                                    organisasjonsnummer = organisasjonsnummer,
                                    omregnetÅrsinntekt = 10000.0,
                                    innrapportertÅrsinntekt = 10000.0,
                                ),
                            ),
                    ),
                skjønnsfastsettingopplysninger = null,
                tags = setOf("IngenNyArbeidsgiverperiode"),
                avslag = AvslagDto(Avslagstype.DELVIS_AVSLAG, "En individuell begrunnelse"),
        )
        vedtakFattetMelder.vedtakFattet(spleis)
        vedtakFattetMelder.publiserUtgåendeMeldinger()
        val eventer = testRapid.inspektør.meldinger()

        assertEquals(1, eventer.size)

        val event = eventer.first()

        assertEquals("vedtak_fattet", event["@event_name"].asText())
        assertEquals(fødselsnummer, event["fødselsnummer"].asText())
        assertEquals(aktørId, event["aktørId"].asText())
        assertEquals(vedtaksperiodeId.toString(), event["vedtaksperiodeId"].asText())
        assertEquals(organisasjonsnummer, event["organisasjonsnummer"].asText())
        assertEquals(fom, event["fom"].asLocalDate())
        assertEquals(tom, event["tom"].asLocalDate())
        assertEquals(skjæringstidspunkt, event["skjæringstidspunkt"].asLocalDate())
        assertEquals(hendelser, event["hendelser"].map { UUID.fromString(it.asText()) })
        assertEquals(10000.0, event["sykepengegrunnlag"].asDouble())
        assertEquals(10000.0, event["grunnlagForSykepengegrunnlag"].asDouble())
        assertEquals(
            mapOf(organisasjonsnummer to 10000.0),
            objectMapper.convertValue(event["grunnlagForSykepengegrunnlagPerArbeidsgiver"]),
        )
        assertEquals("ER_IKKE_6G_BEGRENSET", event["begrensning"].asText())
        assertEquals(10000.0, event["inntekt"].asDouble())
        assertEquals(vedtakFattetTidspunkt, event["vedtakFattetTidspunkt"].asLocalDateTime())
        assertEquals(utbetalingId.toString(), event["utbetalingId"].asText())
        assertEquals("EtterHovedregel", event["sykepengegrunnlagsfakta"]["fastsatt"].asText())
        assertEquals(10000.0, event["sykepengegrunnlagsfakta"]["omregnetÅrsinntekt"].asDouble())
        assertEquals(10000.0, event["sykepengegrunnlagsfakta"]["innrapportertÅrsinntekt"].asDouble())
        assertEquals(0.0, event["sykepengegrunnlagsfakta"]["avviksprosent"].asDouble())
        assertEquals(711720.0, event["sykepengegrunnlagsfakta"]["6G"].asDouble())
        assertEquals(emptyList<String>(), objectMapper.convertValue(event["sykepengegrunnlagsfakta"]["tags"]))
        assertEquals(
            listOf(
                mapOf(
                    "arbeidsgiver" to organisasjonsnummer,
                    "omregnetÅrsinntekt" to 10000.0,
                    "innrapportertÅrsinntekt" to 10000.0,
                ),
            ),
            objectMapper.convertValue(event["sykepengegrunnlagsfakta"]["arbeidsgivere"]),
        )
        assertEquals(0, event["begrunnelser"].size())
        assertEquals(1, event["tags"].size())
        assertEquals("IngenNyArbeidsgiverperiode", event["tags"].first().asText())
        assertEquals(Avslagstype.DELVIS_AVSLAG, enumValueOf<Avslagstype>(event["avslag"]["type"].asText()))
        assertEquals("En individuell begrunnelse", event["avslag"]["begrunnelse"].asText())
    }

    @Test
    fun `vanlig vedtak sykepengegrunnlag fastsatt etter skjønn`() {
        val infotrygd =
            Sykepengevedtak.Vedtak(
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                vedtaksperiodeId = vedtaksperiodeId,
                organisasjonsnummer = organisasjonsnummer,
                spleisBehandlingId = spleisBehandlingId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                hendelser = hendelser,
                sykepengegrunnlag = 10000.0,
                grunnlagForSykepengegrunnlag = 10000.0,
                grunnlagForSykepengegrunnlagPerArbeidsgiver = mapOf(organisasjonsnummer to 10000.0),
                begrensning = "ER_IKKE_6G_BEGRENSET",
                inntekt = 10000.0,
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                utbetalingId = utbetalingId,
                sykepengegrunnlagsfakta =
                    Sykepengegrunnlagsfakta.Spleis.EtterSkjønn(
                        omregnetÅrsinntekt = 10000.0,
                        innrapportertÅrsinntekt = 13000.0,
                        avviksprosent = 30.0,
                        seksG = 711720.0,
                        tags = mutableSetOf(),
                        arbeidsgivere =
                            listOf(
                                Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterSkjønn(
                                    organisasjonsnummer = organisasjonsnummer,
                                    omregnetÅrsinntekt = 10000.0,
                                    skjønnsfastsatt = 13000.0,
                                    innrapportertÅrsinntekt = 13000.0,
                                ),
                            ),
                        skjønnsfastsatt = 13000.0,
                    ),
                skjønnsfastsettingopplysninger =
                    SkjønnsfastsettingopplysningerDto(
                        "Mal",
                        "Fritekst",
                        "Konklusjon",
                        Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT,
                        Skjønnsfastsettingsårsak.ANDRE_AVSNITT,
                    ),
                tags = setOf("IngenNyArbeidsgiverperiode"),
            avslag = null,
        )
        vedtakFattetMelder.vedtakFattet(infotrygd)
        vedtakFattetMelder.publiserUtgåendeMeldinger()
        val eventer = testRapid.inspektør.meldinger()

        assertEquals(1, eventer.size)

        val event = eventer.first()

        assertEquals("vedtak_fattet", event["@event_name"].asText())
        assertEquals(fødselsnummer, event["fødselsnummer"].asText())
        assertEquals(aktørId, event["aktørId"].asText())
        assertEquals(vedtaksperiodeId.toString(), event["vedtaksperiodeId"].asText())
        assertEquals(organisasjonsnummer, event["organisasjonsnummer"].asText())
        assertEquals(fom, event["fom"].asLocalDate())
        assertEquals(tom, event["tom"].asLocalDate())
        assertEquals(skjæringstidspunkt, event["skjæringstidspunkt"].asLocalDate())
        assertEquals(hendelser, event["hendelser"].map { UUID.fromString(it.asText()) })
        assertEquals(10000.0, event["sykepengegrunnlag"].asDouble())
        assertEquals(10000.0, event["grunnlagForSykepengegrunnlag"].asDouble())
        assertEquals(
            mapOf(organisasjonsnummer to 10000.0),
            objectMapper.convertValue(event["grunnlagForSykepengegrunnlagPerArbeidsgiver"]),
        )
        assertEquals("ER_IKKE_6G_BEGRENSET", event["begrensning"].asText())
        assertEquals(10000.0, event["inntekt"].asDouble())
        assertEquals(vedtakFattetTidspunkt, event["vedtakFattetTidspunkt"].asLocalDateTime())
        assertEquals(utbetalingId.toString(), event["utbetalingId"].asText())
        assertEquals("EtterSkjønn", event["sykepengegrunnlagsfakta"]["fastsatt"].asText())
        assertEquals(10000.0, event["sykepengegrunnlagsfakta"]["omregnetÅrsinntekt"].asDouble())
        assertEquals(13000.0, event["sykepengegrunnlagsfakta"]["innrapportertÅrsinntekt"].asDouble())
        assertEquals(30.0, event["sykepengegrunnlagsfakta"]["avviksprosent"].asDouble())
        assertEquals(711720.0, event["sykepengegrunnlagsfakta"]["6G"].asDouble())
        assertEquals(emptyList<String>(), objectMapper.convertValue(event["sykepengegrunnlagsfakta"]["tags"]))
        assertEquals(
            listOf(
                mapOf(
                    "arbeidsgiver" to organisasjonsnummer,
                    "omregnetÅrsinntekt" to 10000.0,
                    "innrapportertÅrsinntekt" to 13000.0,
                    "skjønnsfastsatt" to 13000.0,
                ),
            ),
            objectMapper.convertValue(event["sykepengegrunnlagsfakta"]["arbeidsgivere"]),
        )
        assertEquals(13000.0, event["sykepengegrunnlagsfakta"]["skjønnsfastsatt"].asDouble())

        assertEquals(3, event["begrunnelser"].size())

        assertEquals("SkjønnsfastsattSykepengegrunnlagMal", event["begrunnelser"][0]["type"].asText())
        assertEquals("Mal", event["begrunnelser"][0]["begrunnelse"].asText())
        assertEquals(
            listOf(mapOf("fom" to fom, "tom" to tom)),
            objectMapper.convertValue<List<Map<String, LocalDate>>>(event["begrunnelser"][0]["perioder"]),
        )

        assertEquals("SkjønnsfastsattSykepengegrunnlagFritekst", event["begrunnelser"][1]["type"].asText())
        assertEquals("Fritekst", event["begrunnelser"][1]["begrunnelse"].asText())
        assertEquals(
            listOf(mapOf("fom" to fom, "tom" to tom)),
            objectMapper.convertValue<List<Map<String, LocalDate>>>(event["begrunnelser"][1]["perioder"]),
        )

        assertEquals("SkjønnsfastsattSykepengegrunnlagKonklusjon", event["begrunnelser"][2]["type"].asText())
        assertEquals("Konklusjon", event["begrunnelser"][2]["begrunnelse"].asText())
        assertEquals(
            listOf(mapOf("fom" to fom, "tom" to tom)),
            objectMapper.convertValue<List<Map<String, LocalDate>>>(event["begrunnelser"][2]["perioder"]),
        )

        assertEquals(1, event["tags"].size())
        assertEquals("IngenNyArbeidsgiverperiode", event["tags"].first().asText())
    }

    @Test
    fun `vanlig vedtak sykepengegrunnlag fastsatt etter skjønn med avslag`() {
        val infotrygd =
            Sykepengevedtak.Vedtak(
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                vedtaksperiodeId = vedtaksperiodeId,
                organisasjonsnummer = organisasjonsnummer,
                spleisBehandlingId = spleisBehandlingId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                hendelser = hendelser,
                sykepengegrunnlag = 10000.0,
                grunnlagForSykepengegrunnlag = 10000.0,
                grunnlagForSykepengegrunnlagPerArbeidsgiver = mapOf(organisasjonsnummer to 10000.0),
                begrensning = "ER_IKKE_6G_BEGRENSET",
                inntekt = 10000.0,
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                utbetalingId = utbetalingId,
                sykepengegrunnlagsfakta =
                    Sykepengegrunnlagsfakta.Spleis.EtterSkjønn(
                        omregnetÅrsinntekt = 10000.0,
                        innrapportertÅrsinntekt = 13000.0,
                        avviksprosent = 30.0,
                        seksG = 711720.0,
                        tags = mutableSetOf(),
                        arbeidsgivere =
                            listOf(
                                Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterSkjønn(
                                    organisasjonsnummer = organisasjonsnummer,
                                    omregnetÅrsinntekt = 10000.0,
                                    skjønnsfastsatt = 13000.0,
                                    innrapportertÅrsinntekt = 13000.0,
                                ),
                            ),
                        skjønnsfastsatt = 13000.0,
                    ),
                skjønnsfastsettingopplysninger =
                    SkjønnsfastsettingopplysningerDto(
                        "Mal",
                        "Fritekst",
                        "Konklusjon",
                        Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT,
                        Skjønnsfastsettingsårsak.ANDRE_AVSNITT,
                    ),
                tags = setOf("IngenNyArbeidsgiverperiode"),
            avslag = AvslagDto(
                Avslagstype.AVSLAG,
                "En individuell begrunnelse"
            ),
        )
        vedtakFattetMelder.vedtakFattet(infotrygd)
        vedtakFattetMelder.publiserUtgåendeMeldinger()
        val eventer = testRapid.inspektør.meldinger()

        assertEquals(1, eventer.size)

        val event = eventer.first()

        assertEquals("vedtak_fattet", event["@event_name"].asText())
        assertEquals(fødselsnummer, event["fødselsnummer"].asText())
        assertEquals(aktørId, event["aktørId"].asText())
        assertEquals(vedtaksperiodeId.toString(), event["vedtaksperiodeId"].asText())
        assertEquals(organisasjonsnummer, event["organisasjonsnummer"].asText())
        assertEquals(fom, event["fom"].asLocalDate())
        assertEquals(tom, event["tom"].asLocalDate())
        assertEquals(skjæringstidspunkt, event["skjæringstidspunkt"].asLocalDate())
        assertEquals(hendelser, event["hendelser"].map { UUID.fromString(it.asText()) })
        assertEquals(10000.0, event["sykepengegrunnlag"].asDouble())
        assertEquals(10000.0, event["grunnlagForSykepengegrunnlag"].asDouble())
        assertEquals(
            mapOf(organisasjonsnummer to 10000.0),
            objectMapper.convertValue(event["grunnlagForSykepengegrunnlagPerArbeidsgiver"]),
        )
        assertEquals("ER_IKKE_6G_BEGRENSET", event["begrensning"].asText())
        assertEquals(10000.0, event["inntekt"].asDouble())
        assertEquals(vedtakFattetTidspunkt, event["vedtakFattetTidspunkt"].asLocalDateTime())
        assertEquals(utbetalingId.toString(), event["utbetalingId"].asText())
        assertEquals("EtterSkjønn", event["sykepengegrunnlagsfakta"]["fastsatt"].asText())
        assertEquals(10000.0, event["sykepengegrunnlagsfakta"]["omregnetÅrsinntekt"].asDouble())
        assertEquals(13000.0, event["sykepengegrunnlagsfakta"]["innrapportertÅrsinntekt"].asDouble())
        assertEquals(30.0, event["sykepengegrunnlagsfakta"]["avviksprosent"].asDouble())
        assertEquals(711720.0, event["sykepengegrunnlagsfakta"]["6G"].asDouble())
        assertEquals(emptyList<String>(), objectMapper.convertValue(event["sykepengegrunnlagsfakta"]["tags"]))
        assertEquals(
            listOf(
                mapOf(
                    "arbeidsgiver" to organisasjonsnummer,
                    "omregnetÅrsinntekt" to 10000.0,
                    "innrapportertÅrsinntekt" to 13000.0,
                    "skjønnsfastsatt" to 13000.0,
                ),
            ),
            objectMapper.convertValue(event["sykepengegrunnlagsfakta"]["arbeidsgivere"]),
        )
        assertEquals(13000.0, event["sykepengegrunnlagsfakta"]["skjønnsfastsatt"].asDouble())

        assertEquals(3, event["begrunnelser"].size())

        assertEquals("SkjønnsfastsattSykepengegrunnlagMal", event["begrunnelser"][0]["type"].asText())
        assertEquals("Mal", event["begrunnelser"][0]["begrunnelse"].asText())
        assertEquals(
            listOf(mapOf("fom" to fom, "tom" to tom)),
            objectMapper.convertValue<List<Map<String, LocalDate>>>(event["begrunnelser"][0]["perioder"]),
        )

        assertEquals("SkjønnsfastsattSykepengegrunnlagFritekst", event["begrunnelser"][1]["type"].asText())
        assertEquals("Fritekst", event["begrunnelser"][1]["begrunnelse"].asText())
        assertEquals(
            listOf(mapOf("fom" to fom, "tom" to tom)),
            objectMapper.convertValue<List<Map<String, LocalDate>>>(event["begrunnelser"][1]["perioder"]),
        )

        assertEquals("SkjønnsfastsattSykepengegrunnlagKonklusjon", event["begrunnelser"][2]["type"].asText())
        assertEquals("Konklusjon", event["begrunnelser"][2]["begrunnelse"].asText())
        assertEquals(
            listOf(mapOf("fom" to fom, "tom" to tom)),
            objectMapper.convertValue<List<Map<String, LocalDate>>>(event["begrunnelser"][2]["perioder"]),
        )

        assertEquals(1, event["tags"].size())
        assertEquals("IngenNyArbeidsgiverperiode", event["tags"].first().asText())
        assertEquals(Avslagstype.AVSLAG, enumValueOf<Avslagstype>(event["avslag"]["type"].asText()))
        assertEquals("En individuell begrunnelse", event["avslag"]["begrunnelse"].asText())
    }
}
