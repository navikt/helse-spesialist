package no.nav.helse.mediator

import com.fasterxml.jackson.module.kotlin.convertValue
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.TestRapidHelpers.meldinger
import no.nav.helse.db.SessionContext
import no.nav.helse.kafka.MessageContextMeldingPubliserer
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.melding.Sykepengevedtak
import no.nav.helse.modell.melding.Sykepengevedtak.VedtakMedSkjønnsvurdering
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.vedtak.Skjønnsfastsettingstype
import no.nav.helse.modell.vedtak.Skjønnsfastsettingsårsak
import no.nav.helse.modell.vedtak.Sykepengegrunnlagsfakta
import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.modell.vedtak.VedtakBegrunnelse
import no.nav.helse.modell.vilkårsprøving.InnrapportertInntekt
import no.nav.helse.modell.vilkårsprøving.Inntekt
import no.nav.helse.modell.vilkårsprøving.Sammenligningsgrunnlag
import no.nav.helse.spesialist.kafka.objectMapper
import no.nav.helse.spesialist.testfixtures.jan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

internal class PubliserSykepengevedtakTest {
    private val testRapid = TestRapid()
    private val publiserer = MessageContextMeldingPubliserer(testRapid)
    private val utgåendeMeldingerMediator = UtgåendeMeldingerMediator()

    private companion object {
        private const val FØDSELSNUMMER = "12345678910"
        private val vedtaksperiodeId = UUID.randomUUID()
        private val spleisBehandlingId = UUID.randomUUID()
        private val utbetalingId = UUID.randomUUID()
        private const val ORGANISASJONSNUMMER = "123456789"
        private const val AKTØRID = "1234567891011"
        private val fom = 1 jan 2018
        private val tom = 31 jan 2018
        private val skjæringstidspunkt = 1 jan 2018
        private val hendelser = listOf(UUID.randomUUID())
        private val vedtakFattetTidspunkt = LocalDateTime.now()
    }

    private val personmelding = object: Personmelding {
        override fun behandle(
            person: Person,
            kommandostarter: Kommandostarter,
            sessionContext: SessionContext,
        ) {}

        override fun fødselsnummer(): String = FØDSELSNUMMER

        override val id: UUID = UUID.randomUUID()

        override fun toJson(): String = "{}"

    }

    @Test
    fun `vedtak med opphav i infotrygd`() {
        val infotrygd =
            Sykepengevedtak.VedtakMedOpphavIInfotrygd(
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØRID,
                organisasjonsnummer = ORGANISASJONSNUMMER,
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
                utbetalingId = utbetalingId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                hendelser = hendelser,
                sykepengegrunnlag = 10000.0,
                grunnlagForSykepengegrunnlag = 10000.0,
                grunnlagForSykepengegrunnlagPerArbeidsgiver = mapOf(ORGANISASJONSNUMMER to 10000.0),
                begrensning = "VURDERT_I_INFOTRYGD",
                inntekt = 10000.0,
                sykepengegrunnlagsfakta =
                    Sykepengegrunnlagsfakta.Infotrygd(
                        omregnetÅrsinntekt = 10000.0,
                    ),
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                tags = setOf("IngenNyArbeidsgiverperiode"),
                vedtakBegrunnelse = null,
            )
        utgåendeMeldingerMediator.hendelse(infotrygd)
        utgåendeMeldingerMediator.publiserOppsamledeMeldinger(personmelding, publiserer)
        val eventer = testRapid.inspektør.meldinger()

        assertEquals(1, eventer.size)

        val event = eventer.first()

        assertEquals("vedtak_fattet", event["@event_name"].asText())
        assertEquals(FØDSELSNUMMER, event["fødselsnummer"].asText())
        assertEquals(AKTØRID, event["aktørId"].asText())
        assertEquals(vedtaksperiodeId.toString(), event["vedtaksperiodeId"].asText())
        assertEquals(ORGANISASJONSNUMMER, event["organisasjonsnummer"].asText())
        assertEquals(fom, event["fom"].asLocalDate())
        assertEquals(tom, event["tom"].asLocalDate())
        assertEquals(skjæringstidspunkt, event["skjæringstidspunkt"].asLocalDate())
        assertEquals(hendelser, event["hendelser"].map { UUID.fromString(it.asText()) })
        assertEquals(10000.0, event["sykepengegrunnlag"].asDouble())
        assertEquals(10000.0, event["grunnlagForSykepengegrunnlag"].asDouble())
        assertEquals(
            mapOf(ORGANISASJONSNUMMER to 10000.0),
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
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØRID,
                organisasjonsnummer = ORGANISASJONSNUMMER,
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
                utbetalingId = utbetalingId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                hendelser = hendelser,
                sykepengegrunnlag = 10000.0,
                grunnlagForSykepengegrunnlag = 10000.0,
                grunnlagForSykepengegrunnlagPerArbeidsgiver = mapOf(ORGANISASJONSNUMMER to 10000.0),
                begrensning = "ER_IKKE_6G_BEGRENSET",
                inntekt = 10000.0,
                sykepengegrunnlagsfakta =
                    Sykepengegrunnlagsfakta.Spleis.EtterHovedregel(
                        omregnetÅrsinntekt = 10000.0,
                        seksG = 711720.0,
                        tags = mutableSetOf(),
                        arbeidsgivere =
                            listOf(
                                Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterHovedregel(
                                    organisasjonsnummer = ORGANISASJONSNUMMER,
                                    omregnetÅrsinntekt = 10000.0,
                                ),
                            ),
                    ),
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                tags = setOf("IngenNyArbeidsgiverperiode"),
                vedtakBegrunnelse = null,
                avviksprosent = 0.0,
                sammenligningsgrunnlag = sammenligningsgrunnlag(10000.0, ORGANISASJONSNUMMER),
                )
        utgåendeMeldingerMediator.hendelse(infotrygd)
        utgåendeMeldingerMediator.publiserOppsamledeMeldinger(personmelding, publiserer)
        val eventer = testRapid.inspektør.meldinger()

        assertEquals(1, eventer.size)

        val event = eventer.first()

        assertEquals("vedtak_fattet", event["@event_name"].asText())
        assertEquals(FØDSELSNUMMER, event["fødselsnummer"].asText())
        assertEquals(AKTØRID, event["aktørId"].asText())
        assertEquals(vedtaksperiodeId.toString(), event["vedtaksperiodeId"].asText())
        assertEquals(ORGANISASJONSNUMMER, event["organisasjonsnummer"].asText())
        assertEquals(fom, event["fom"].asLocalDate())
        assertEquals(tom, event["tom"].asLocalDate())
        assertEquals(skjæringstidspunkt, event["skjæringstidspunkt"].asLocalDate())
        assertEquals(hendelser, event["hendelser"].map { UUID.fromString(it.asText()) })
        assertEquals(10000.0, event["sykepengegrunnlag"].asDouble())
        assertEquals(10000.0, event["grunnlagForSykepengegrunnlag"].asDouble())
        assertEquals(
            mapOf(ORGANISASJONSNUMMER to 10000.0),
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
                    "arbeidsgiver" to ORGANISASJONSNUMMER,
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
    fun `vanlig vedtak sykepengegrunnlag fastsatt etter hovedregel med delvis innvilgelse`() {
        val spleis =
            Sykepengevedtak.Vedtak(
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØRID,
                organisasjonsnummer = ORGANISASJONSNUMMER,
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
                utbetalingId = utbetalingId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                hendelser = hendelser,
                sykepengegrunnlag = 10000.0,
                grunnlagForSykepengegrunnlag = 10000.0,
                grunnlagForSykepengegrunnlagPerArbeidsgiver = mapOf(ORGANISASJONSNUMMER to 10000.0),
                begrensning = "ER_IKKE_6G_BEGRENSET",
                inntekt = 10000.0,
                sykepengegrunnlagsfakta =
                    Sykepengegrunnlagsfakta.Spleis.EtterHovedregel(
                        omregnetÅrsinntekt = 10000.0,
                        seksG = 711720.0,
                        tags = mutableSetOf(),
                        arbeidsgivere =
                            listOf(
                                Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterHovedregel(
                                    organisasjonsnummer = ORGANISASJONSNUMMER,
                                    omregnetÅrsinntekt = 10000.0,
                                ),
                            ),
                    ),
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                tags = setOf("IngenNyArbeidsgiverperiode"),
                vedtakBegrunnelse = VedtakBegrunnelse(
                    Utfall.DELVIS_INNVILGELSE,
                    "En individuell begrunnelse"
                ),
                avviksprosent = 0.0,
                sammenligningsgrunnlag = sammenligningsgrunnlag(10000.0, ORGANISASJONSNUMMER),
            )
        utgåendeMeldingerMediator.hendelse(spleis)
        utgåendeMeldingerMediator.publiserOppsamledeMeldinger(personmelding, publiserer)
        val eventer = testRapid.inspektør.meldinger()

        assertEquals(1, eventer.size)

        val event = eventer.first()

        assertEquals("vedtak_fattet", event["@event_name"].asText())
        assertEquals(FØDSELSNUMMER, event["fødselsnummer"].asText())
        assertEquals(AKTØRID, event["aktørId"].asText())
        assertEquals(vedtaksperiodeId.toString(), event["vedtaksperiodeId"].asText())
        assertEquals(ORGANISASJONSNUMMER, event["organisasjonsnummer"].asText())
        assertEquals(fom, event["fom"].asLocalDate())
        assertEquals(tom, event["tom"].asLocalDate())
        assertEquals(skjæringstidspunkt, event["skjæringstidspunkt"].asLocalDate())
        assertEquals(hendelser, event["hendelser"].map { UUID.fromString(it.asText()) })
        assertEquals(10000.0, event["sykepengegrunnlag"].asDouble())
        assertEquals(10000.0, event["grunnlagForSykepengegrunnlag"].asDouble())
        assertEquals(
            mapOf(ORGANISASJONSNUMMER to 10000.0),
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
                    "arbeidsgiver" to ORGANISASJONSNUMMER,
                    "omregnetÅrsinntekt" to 10000.0,
                    "innrapportertÅrsinntekt" to 10000.0,
                ),
            ),
            objectMapper.convertValue(event["sykepengegrunnlagsfakta"]["arbeidsgivere"]),
        )

        assertEquals(1, event["begrunnelser"].size())
        assertEquals(1, event["tags"].size())
        assertEquals("IngenNyArbeidsgiverperiode", event["tags"].first().asText())
        assertEquals("DelvisInnvilgelse", event["begrunnelser"][0]["type"].asText())
        assertEquals("En individuell begrunnelse", event["begrunnelser"][0]["begrunnelse"].asText())
        assertEquals(
            listOf(mapOf("fom" to fom, "tom" to tom)),
            objectMapper.convertValue<List<Map<String, LocalDate>>>(event["begrunnelser"][0]["perioder"]),
        )
    }

    @Test
    fun `vanlig vedtak sykepengegrunnlag fastsatt etter skjønn`() {
        val infotrygd =
            VedtakMedSkjønnsvurdering(
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØRID,
                organisasjonsnummer = ORGANISASJONSNUMMER,
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
                utbetalingId = utbetalingId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                hendelser = hendelser,
                sykepengegrunnlag = 10000.0,
                grunnlagForSykepengegrunnlag = 10000.0,
                grunnlagForSykepengegrunnlagPerArbeidsgiver = mapOf(ORGANISASJONSNUMMER to 10000.0),
                begrensning = "ER_IKKE_6G_BEGRENSET",
                inntekt = 10000.0,
                sykepengegrunnlagsfakta =
                    Sykepengegrunnlagsfakta.Spleis.EtterSkjønn(
                        omregnetÅrsinntekt = 10000.0,
                        seksG = 711720.0,
                        tags = mutableSetOf(),
                        arbeidsgivere =
                            listOf(
                                Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterSkjønn(
                                    organisasjonsnummer = ORGANISASJONSNUMMER,
                                    omregnetÅrsinntekt = 10000.0,
                                    skjønnsfastsatt = 13000.0,
                                ),
                            ),
                        skjønnsfastsatt = 13000.0,
                    ),
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
                vedtakBegrunnelse = null,
                avviksprosent = 30.0,
                sammenligningsgrunnlag = sammenligningsgrunnlag(12000.0, ORGANISASJONSNUMMER),
            )
        utgåendeMeldingerMediator.hendelse(infotrygd)
        utgåendeMeldingerMediator.publiserOppsamledeMeldinger(personmelding, publiserer)
        val eventer = testRapid.inspektør.meldinger()

        assertEquals(1, eventer.size)

        val event = eventer.first()

        assertEquals("vedtak_fattet", event["@event_name"].asText())
        assertEquals(FØDSELSNUMMER, event["fødselsnummer"].asText())
        assertEquals(AKTØRID, event["aktørId"].asText())
        assertEquals(vedtaksperiodeId.toString(), event["vedtaksperiodeId"].asText())
        assertEquals(ORGANISASJONSNUMMER, event["organisasjonsnummer"].asText())
        assertEquals(fom, event["fom"].asLocalDate())
        assertEquals(tom, event["tom"].asLocalDate())
        assertEquals(skjæringstidspunkt, event["skjæringstidspunkt"].asLocalDate())
        assertEquals(hendelser, event["hendelser"].map { UUID.fromString(it.asText()) })
        assertEquals(10000.0, event["sykepengegrunnlag"].asDouble())
        assertEquals(10000.0, event["grunnlagForSykepengegrunnlag"].asDouble())
        assertEquals(
            mapOf(ORGANISASJONSNUMMER to 10000.0),
            objectMapper.convertValue(event["grunnlagForSykepengegrunnlagPerArbeidsgiver"]),
        )
        assertEquals("ER_IKKE_6G_BEGRENSET", event["begrensning"].asText())
        assertEquals(10000.0, event["inntekt"].asDouble())
        assertEquals(vedtakFattetTidspunkt, event["vedtakFattetTidspunkt"].asLocalDateTime())
        assertEquals(utbetalingId.toString(), event["utbetalingId"].asText())
        assertEquals("EtterSkjønn", event["sykepengegrunnlagsfakta"]["fastsatt"].asText())
        assertEquals(10000.0, event["sykepengegrunnlagsfakta"]["omregnetÅrsinntekt"].asDouble())
        assertEquals(12000.0, event["sykepengegrunnlagsfakta"]["innrapportertÅrsinntekt"].asDouble())
        assertEquals(30.0, event["sykepengegrunnlagsfakta"]["avviksprosent"].asDouble())
        assertEquals(711720.0, event["sykepengegrunnlagsfakta"]["6G"].asDouble())
        assertEquals(emptyList<String>(), objectMapper.convertValue(event["sykepengegrunnlagsfakta"]["tags"]))
        assertEquals(
            listOf(
                mapOf(
                    "arbeidsgiver" to ORGANISASJONSNUMMER,
                    "omregnetÅrsinntekt" to 10000.0,
                    "innrapportertÅrsinntekt" to 12000.0,
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
            VedtakMedSkjønnsvurdering(
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØRID,
                organisasjonsnummer = ORGANISASJONSNUMMER,
                vedtaksperiodeId = vedtaksperiodeId,
                spleisBehandlingId = spleisBehandlingId,
                utbetalingId = utbetalingId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                hendelser = hendelser,
                sykepengegrunnlag = 10000.0,
                grunnlagForSykepengegrunnlag = 10000.0,
                grunnlagForSykepengegrunnlagPerArbeidsgiver = mapOf(ORGANISASJONSNUMMER to 10000.0),
                begrensning = "ER_IKKE_6G_BEGRENSET",
                inntekt = 10000.0,
                sykepengegrunnlagsfakta =
                    Sykepengegrunnlagsfakta.Spleis.EtterSkjønn(
                        omregnetÅrsinntekt = 10000.0,
                        seksG = 711720.0,
                        tags = mutableSetOf(),
                        arbeidsgivere =
                        listOf(
                            Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterSkjønn(
                                organisasjonsnummer = ORGANISASJONSNUMMER,
                                omregnetÅrsinntekt = 10000.0,
                                skjønnsfastsatt = 13000.0,
                            ),
                        ),
                        skjønnsfastsatt = 13000.0,
                    ),
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
                vedtakBegrunnelse = VedtakBegrunnelse(Utfall.AVSLAG, "En individuell begrunnelse"),
                avviksprosent = 30.0,
                sammenligningsgrunnlag = sammenligningsgrunnlag(13000.0, ORGANISASJONSNUMMER),
            )
        utgåendeMeldingerMediator.hendelse(infotrygd)
        utgåendeMeldingerMediator.publiserOppsamledeMeldinger(personmelding, publiserer)
        val eventer = testRapid.inspektør.meldinger()

        assertEquals(1, eventer.size)

        val event = eventer.first()

        assertEquals("vedtak_fattet", event["@event_name"].asText())
        assertEquals(FØDSELSNUMMER, event["fødselsnummer"].asText())
        assertEquals(AKTØRID, event["aktørId"].asText())
        assertEquals(vedtaksperiodeId.toString(), event["vedtaksperiodeId"].asText())
        assertEquals(ORGANISASJONSNUMMER, event["organisasjonsnummer"].asText())
        assertEquals(fom, event["fom"].asLocalDate())
        assertEquals(tom, event["tom"].asLocalDate())
        assertEquals(skjæringstidspunkt, event["skjæringstidspunkt"].asLocalDate())
        assertEquals(hendelser, event["hendelser"].map { UUID.fromString(it.asText()) })
        assertEquals(10000.0, event["sykepengegrunnlag"].asDouble())
        assertEquals(10000.0, event["grunnlagForSykepengegrunnlag"].asDouble())
        assertEquals(
            mapOf(ORGANISASJONSNUMMER to 10000.0),
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
                    "arbeidsgiver" to ORGANISASJONSNUMMER,
                    "omregnetÅrsinntekt" to 10000.0,
                    "innrapportertÅrsinntekt" to 13000.0,
                    "skjønnsfastsatt" to 13000.0,
                ),
            ),
            objectMapper.convertValue(event["sykepengegrunnlagsfakta"]["arbeidsgivere"]),
        )
        assertEquals(13000.0, event["sykepengegrunnlagsfakta"]["skjønnsfastsatt"].asDouble())

        assertEquals(4, event["begrunnelser"].size())

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

        assertEquals("Avslag", event["begrunnelser"][3]["type"].asText())
        assertEquals("En individuell begrunnelse", event["begrunnelser"][3]["begrunnelse"].asText())
        assertEquals(
            listOf(mapOf("fom" to fom, "tom" to tom)),
            objectMapper.convertValue<List<Map<String, LocalDate>>>(event["begrunnelser"][3]["perioder"]),
        )

        assertEquals(1, event["tags"].size())
        assertEquals("IngenNyArbeidsgiverperiode", event["tags"].first().asText())
    }

    private fun sammenligningsgrunnlag(
        totalbeløp: Double,
        arbeidsgiver: String
    ): Sammenligningsgrunnlag {
        val yearMonth = YearMonth.from(skjæringstidspunkt.minusMonths(1))
        return Sammenligningsgrunnlag(
            totalbeløp = totalbeløp,
            innrapporterteInntekter = listOf(
                InnrapportertInntekt(arbeidsgiver, listOf(Inntekt(yearMonth, totalbeløp)))
            )
        )
    }
}
