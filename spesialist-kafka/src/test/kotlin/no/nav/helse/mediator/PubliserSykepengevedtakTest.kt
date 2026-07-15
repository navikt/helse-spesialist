package no.nav.helse.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.db.SessionContext
import no.nav.helse.kafka.MessageContextMeldingPubliserer
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.melding.SaksbehandlerIdentOgNavn
import no.nav.helse.modell.melding.VedtakFattetMelding
import no.nav.helse.modell.person.LegacyPerson
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.kafka.TestRapidHelpers.meldinger
import no.nav.helse.spesialist.kafka.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.convertValue
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class PubliserSykepengevedtakTest {
    private val testRapid = TestRapid()
    private val publiserer = MessageContextMeldingPubliserer(testRapid)
    private val utgåendeMeldingerMediator = UtgåendeMeldingerMediator()
    private val saksbehandlerIdentOgNavn = lagSaksbehandler().let { SaksbehandlerIdentOgNavn(it.ident.value, it.navn) }
    private val beslutterIdentOgNavn = lagSaksbehandler().let { SaksbehandlerIdentOgNavn(it.ident.value, it.navn) }

    private companion object {
        private const val FØDSELSNUMMER = "12345678910"
        private val vedtaksperiodeId = UUID.randomUUID()
        private val spleisBehandlingId = UUID.randomUUID()
        private val utbetalingId = UUID.randomUUID()
        private const val ORGANISASJONSNUMMER = "123456789"
        private const val YRKESAKTIVITETSTYPE = "ARBEIDSTAKER"
        private const val AKTØRID = "1234567891011"
        private val fom = 1 jan 2018
        private val tom = 31 jan 2018
        private val skjæringstidspunkt = 1 jan 2018
        private val hendelser = listOf(UUID.randomUUID())
        private val vedtakFattetTidspunkt = LocalDateTime.now()
    }

    private val personmelding =
        object : Personmelding {
            override fun behandleMedLegacyPerson(
                person: LegacyPerson,
                kommandostarter: Kommandostarter,
                sessionContext: SessionContext,
            ) {
            }

            override fun fødselsnummer(): String = FØDSELSNUMMER

            override val id: UUID = UUID.randomUUID()

            override fun toJson(): String = "{}"
        }

    @Test
    fun `vedtak med opphav i infotrygd`() {
        val infotrygd =
            VedtakFattetMelding(
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØRID,
                organisasjonsnummer = ORGANISASJONSNUMMER,
                yrkesaktivitetstype = YRKESAKTIVITETSTYPE,
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = spleisBehandlingId,
                utbetalingId = utbetalingId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                hendelser = hendelser,
                sykepengegrunnlag = BigDecimal("10000.0"),
                sykepengegrunnlagsfakta =
                    VedtakFattetMelding.FastsattIInfotrygdSykepengegrunnlagsfakta(
                        omregnetÅrsinntekt = BigDecimal("10000.0"),
                    ),
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                tags = setOf("IngenNyArbeidsgiverperiode"),
                begrunnelser = emptyList(),
                beslutter = beslutterIdentOgNavn,
                saksbehandler = saksbehandlerIdentOgNavn,
                automatiskFattet = false,
                dekning = null,
                forsikringsvurderingId = null,
                utbetalingsdager = listOf(mapOf("utbetaling" to "medFriStruktur")),
            )
        utgåendeMeldingerMediator.hendelse(infotrygd)
        utgåendeMeldingerMediator.publiserOppsamledeMeldinger(personmelding, publiserer)
        val eventer = testRapid.inspektør.meldinger()

        assertEquals(1, eventer.size)

        val event = eventer.first()

        assertEquals("vedtak_fattet", event["@event_name"].asString())
        assertEquals(FØDSELSNUMMER, event["fødselsnummer"].asString())
        assertEquals(AKTØRID, event["aktørId"].asString())
        assertEquals(vedtaksperiodeId.toString(), event["vedtaksperiodeId"].asString())
        assertEquals(ORGANISASJONSNUMMER, event["organisasjonsnummer"].asString())
        assertEquals(fom, event["fom"].asLocalDate())
        assertEquals(tom, event["tom"].asLocalDate())
        assertEquals(skjæringstidspunkt, event["skjæringstidspunkt"].asLocalDate())
        assertEquals(hendelser, event["hendelser"].toList().map { UUID.fromString(it.asString()) })
        assertEquals("10000.0", event["sykepengegrunnlag"].asString())
        assertEquals(vedtakFattetTidspunkt, event["vedtakFattetTidspunkt"].asLocalDateTime())
        assertEquals(utbetalingId.toString(), event["utbetalingId"].asString())
        assertEquals("IInfotrygd", event["sykepengegrunnlagsfakta"]["fastsatt"].asString())
        assertEquals("10000.0", event["sykepengegrunnlagsfakta"]["omregnetÅrsinntekt"].asString())
        assertEquals(0, event["begrunnelser"].size())
        assertEquals(1, event["tags"].size())
        assertEquals("IngenNyArbeidsgiverperiode", event["tags"].first().asString())
        assertEquals(saksbehandlerIdentOgNavn.ident, event["saksbehandler"]["ident"].asString())
        assertEqualsNavn(saksbehandlerIdentOgNavn.navn, event["saksbehandler"]["navn"].asString())
        assertEquals(beslutterIdentOgNavn.ident, event["beslutter"]["ident"].asString())
        assertEqualsNavn(beslutterIdentOgNavn.navn, event["beslutter"]["navn"].asString())
        assertEquals(false, event["automatiskFattet"].asBoolean())
    }

    @Test
    fun `vanlig vedtak sykepengegrunnlag fastsatt etter hovedregel`() {
        val infotrygd =
            VedtakFattetMelding(
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØRID,
                organisasjonsnummer = ORGANISASJONSNUMMER,
                yrkesaktivitetstype = YRKESAKTIVITETSTYPE,
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = spleisBehandlingId,
                utbetalingId = utbetalingId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                hendelser = hendelser,
                sykepengegrunnlag = BigDecimal("10000.0"),
                sykepengegrunnlagsfakta =
                    VedtakFattetMelding.FastsattEtterHovedregelSykepengegrunnlagsfakta(
                        omregnetÅrsinntekt = BigDecimal("10000.0"),
                        seksG = BigDecimal("711720.0"),
                        avviksprosent = BigDecimal("0.0"),
                        innrapportertÅrsinntekt = BigDecimal("10000.0"),
                        tags = setOf(),
                        arbeidsgivere =
                            listOf(
                                VedtakFattetMelding.FastsattEtterHovedregelSykepengegrunnlagsfakta.Arbeidsgiver(
                                    organisasjonsnummer = ORGANISASJONSNUMMER,
                                    omregnetÅrsinntekt = BigDecimal("10000.0"),
                                    innrapportertÅrsinntekt = BigDecimal("10000.0"),
                                ),
                            ),
                    ),
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                tags = setOf("IngenNyArbeidsgiverperiode"),
                begrunnelser = emptyList(),
                beslutter = beslutterIdentOgNavn,
                saksbehandler = saksbehandlerIdentOgNavn,
                automatiskFattet = false,
                dekning = null,
                forsikringsvurderingId = null,
                utbetalingsdager = listOf(mapOf("utbetaling" to "medFriStruktur")),
            )
        utgåendeMeldingerMediator.hendelse(infotrygd)
        utgåendeMeldingerMediator.publiserOppsamledeMeldinger(personmelding, publiserer)
        val eventer = testRapid.inspektør.meldinger()

        assertEquals(1, eventer.size)

        val event = eventer.first()

        assertEquals("vedtak_fattet", event["@event_name"].asString())
        assertEquals(FØDSELSNUMMER, event["fødselsnummer"].asString())
        assertEquals(AKTØRID, event["aktørId"].asString())
        assertEquals(vedtaksperiodeId.toString(), event["vedtaksperiodeId"].asString())
        assertEquals(ORGANISASJONSNUMMER, event["organisasjonsnummer"].asString())
        assertEquals(fom, event["fom"].asLocalDate())
        assertEquals(tom, event["tom"].asLocalDate())
        assertEquals(skjæringstidspunkt, event["skjæringstidspunkt"].asLocalDate())
        assertEquals(hendelser, event["hendelser"].toList().map { UUID.fromString(it.asString()) })
        assertEquals("10000.0", event["sykepengegrunnlag"].asString())
        assertEquals(vedtakFattetTidspunkt, event["vedtakFattetTidspunkt"].asLocalDateTime())
        assertEquals(utbetalingId.toString(), event["utbetalingId"].asString())
        assertEquals("EtterHovedregel", event["sykepengegrunnlagsfakta"]["fastsatt"].asString())
        assertEquals("10000.0", event["sykepengegrunnlagsfakta"]["omregnetÅrsinntekt"].asString())
        assertEquals("10000.0", event["sykepengegrunnlagsfakta"]["innrapportertÅrsinntekt"].asString())
        assertEquals("0.0", event["sykepengegrunnlagsfakta"]["avviksprosent"].asString())
        assertEquals("711720.0", event["sykepengegrunnlagsfakta"]["6G"].asString())
        assertEquals(emptyList<String>(), objectMapper.convertValue(event["sykepengegrunnlagsfakta"]["tags"]))
        assertEquals(1, event["sykepengegrunnlagsfakta"]["arbeidsgivere"].size())
        assertEquals(ORGANISASJONSNUMMER, event["sykepengegrunnlagsfakta"]["arbeidsgivere"][0]["arbeidsgiver"].asString())
        assertEquals("10000.0", event["sykepengegrunnlagsfakta"]["arbeidsgivere"][0]["omregnetÅrsinntekt"].asString())
        assertEquals("10000.0", event["sykepengegrunnlagsfakta"]["arbeidsgivere"][0]["innrapportertÅrsinntekt"].asString())
        assertEquals(0, event["begrunnelser"].size())
        assertEquals(1, event["tags"].size())
        assertEquals("IngenNyArbeidsgiverperiode", event["tags"].first().asString())
        assertEquals(saksbehandlerIdentOgNavn.ident, event["saksbehandler"]["ident"].asString())
        assertEqualsNavn(saksbehandlerIdentOgNavn.navn, event["saksbehandler"]["navn"].asString())
        assertEquals(beslutterIdentOgNavn.ident, event["beslutter"]["ident"].asString())
        assertEqualsNavn(beslutterIdentOgNavn.navn, event["beslutter"]["navn"].asString())
        assertEquals(false, event["automatiskFattet"].asBoolean())
    }

    @Test
    fun `vanlig vedtak sykepengegrunnlag fastsatt etter hovedregel med delvis innvilgelse`() {
        val spleis =
            VedtakFattetMelding(
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØRID,
                organisasjonsnummer = ORGANISASJONSNUMMER,
                yrkesaktivitetstype = YRKESAKTIVITETSTYPE,
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = spleisBehandlingId,
                utbetalingId = utbetalingId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                hendelser = hendelser,
                sykepengegrunnlag = BigDecimal("10000.0"),
                sykepengegrunnlagsfakta =
                    VedtakFattetMelding.FastsattEtterHovedregelSykepengegrunnlagsfakta(
                        tags = setOf(),
                        omregnetÅrsinntekt = BigDecimal("10000.0"),
                        seksG = BigDecimal("711720.0"),
                        avviksprosent = BigDecimal("0.0"),
                        innrapportertÅrsinntekt = BigDecimal("10000.0"),
                        arbeidsgivere =
                            listOf(
                                VedtakFattetMelding.FastsattEtterHovedregelSykepengegrunnlagsfakta.Arbeidsgiver(
                                    organisasjonsnummer = ORGANISASJONSNUMMER,
                                    omregnetÅrsinntekt = BigDecimal("10000.0"),
                                    innrapportertÅrsinntekt = BigDecimal("10000.0"),
                                ),
                            ),
                    ),
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                tags = setOf("IngenNyArbeidsgiverperiode"),
                begrunnelser =
                    listOf(
                        VedtakFattetMelding.Begrunnelse(
                            type = VedtakFattetMelding.BegrunnelseType.DelvisInnvilgelse,
                            begrunnelse = "En individuell begrunnelse",
                        ),
                    ),
                beslutter = beslutterIdentOgNavn,
                saksbehandler = saksbehandlerIdentOgNavn,
                automatiskFattet = false,
                dekning = null,
                forsikringsvurderingId = null,
                utbetalingsdager = listOf(mapOf("utbetaling" to "medFriStruktur")),
            )
        utgåendeMeldingerMediator.hendelse(spleis)
        utgåendeMeldingerMediator.publiserOppsamledeMeldinger(personmelding, publiserer)
        val eventer = testRapid.inspektør.meldinger()

        assertEquals(1, eventer.size)

        val event = eventer.first()

        assertEquals("vedtak_fattet", event["@event_name"].asString())
        assertEquals(FØDSELSNUMMER, event["fødselsnummer"].asString())
        assertEquals(AKTØRID, event["aktørId"].asString())
        assertEquals(vedtaksperiodeId.toString(), event["vedtaksperiodeId"].asString())
        assertEquals(ORGANISASJONSNUMMER, event["organisasjonsnummer"].asString())
        assertEquals(fom, event["fom"].asLocalDate())
        assertEquals(tom, event["tom"].asLocalDate())
        assertEquals(skjæringstidspunkt, event["skjæringstidspunkt"].asLocalDate())
        assertEquals(hendelser, event["hendelser"].toList().map { UUID.fromString(it.asString()) })
        assertEquals("10000.0", event["sykepengegrunnlag"].asString())
        assertEquals(vedtakFattetTidspunkt, event["vedtakFattetTidspunkt"].asLocalDateTime())
        assertEquals(utbetalingId.toString(), event["utbetalingId"].asString())
        assertEquals("EtterHovedregel", event["sykepengegrunnlagsfakta"]["fastsatt"].asString())
        assertEquals("10000.0", event["sykepengegrunnlagsfakta"]["omregnetÅrsinntekt"].asString())
        assertEquals("10000.0", event["sykepengegrunnlagsfakta"]["innrapportertÅrsinntekt"].asString())
        assertEquals("0.0", event["sykepengegrunnlagsfakta"]["avviksprosent"].asString())
        assertEquals("711720.0", event["sykepengegrunnlagsfakta"]["6G"].asString())
        assertEquals(emptyList<String>(), objectMapper.convertValue(event["sykepengegrunnlagsfakta"]["tags"]))
        assertEquals(1, event["sykepengegrunnlagsfakta"]["arbeidsgivere"].size())
        assertEquals(ORGANISASJONSNUMMER, event["sykepengegrunnlagsfakta"]["arbeidsgivere"][0]["arbeidsgiver"].asString())
        assertEquals("10000.0", event["sykepengegrunnlagsfakta"]["arbeidsgivere"][0]["omregnetÅrsinntekt"].asString())
        assertEquals("10000.0", event["sykepengegrunnlagsfakta"]["arbeidsgivere"][0]["innrapportertÅrsinntekt"].asString())

        assertEquals(1, event["begrunnelser"].size())
        assertEquals(1, event["tags"].size())
        assertEquals("IngenNyArbeidsgiverperiode", event["tags"].first().asString())
        assertEquals("DelvisInnvilgelse", event["begrunnelser"][0]["type"].asString())
        assertEquals("En individuell begrunnelse", event["begrunnelser"][0]["begrunnelse"].asString())
        assertEquals(
            listOf(mapOf("fom" to fom, "tom" to tom)),
            objectMapper.convertValue<List<Map<String, LocalDate>>>(event["begrunnelser"][0]["perioder"]),
        )
        assertEquals(saksbehandlerIdentOgNavn.ident, event["saksbehandler"]["ident"].asString())
        assertEqualsNavn(saksbehandlerIdentOgNavn.navn, event["saksbehandler"]["navn"].asString())
        assertEquals(beslutterIdentOgNavn.ident, event["beslutter"]["ident"].asString())
        assertEqualsNavn(beslutterIdentOgNavn.navn, event["beslutter"]["navn"].asString())
        assertEquals(false, event["automatiskFattet"].asBoolean())
    }

    @Test
    fun `vanlig vedtak sykepengegrunnlag fastsatt etter skjønn`() {
        val infotrygd =
            VedtakFattetMelding(
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØRID,
                organisasjonsnummer = ORGANISASJONSNUMMER,
                yrkesaktivitetstype = YRKESAKTIVITETSTYPE,
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = spleisBehandlingId,
                utbetalingId = utbetalingId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                hendelser = hendelser,
                sykepengegrunnlag = BigDecimal("10000.0"),
                sykepengegrunnlagsfakta =
                    VedtakFattetMelding.FastsattEtterSkjønnSykepengegrunnlagsfakta(
                        omregnetÅrsinntektTotalt = BigDecimal("10000.0"),
                        seksG = BigDecimal("711720.0"),
                        avviksprosent = BigDecimal("30.0"),
                        innrapportertÅrsinntekt = BigDecimal("12000.0"),
                        tags = setOf(),
                        arbeidsgivere =
                            listOf(
                                VedtakFattetMelding.FastsattEtterSkjønnSykepengegrunnlagsfakta.Arbeidsgiver(
                                    organisasjonsnummer = ORGANISASJONSNUMMER,
                                    omregnetÅrsinntekt = BigDecimal("10000.0"),
                                    innrapportertÅrsinntekt = BigDecimal("12000.0"),
                                    skjønnsfastsatt = BigDecimal("13000.0"),
                                ),
                            ),
                        skjønnsfastsatt = BigDecimal("13000.0"),
                        skjønnsfastsettingtype = VedtakFattetMelding.Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT,
                        skjønnsfastsettingsårsak = VedtakFattetMelding.Skjønnsfastsettingsårsak.ANDRE_AVSNITT,
                    ),
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                tags = setOf("IngenNyArbeidsgiverperiode"),
                begrunnelser =
                    listOf(
                        VedtakFattetMelding.Begrunnelse(
                            type = VedtakFattetMelding.BegrunnelseType.SkjønnsfastsattSykepengegrunnlagMal,
                            begrunnelse = "Mal",
                        ),
                        VedtakFattetMelding.Begrunnelse(
                            type = VedtakFattetMelding.BegrunnelseType.SkjønnsfastsattSykepengegrunnlagFritekst,
                            begrunnelse = "Fritekst",
                        ),
                        VedtakFattetMelding.Begrunnelse(
                            type = VedtakFattetMelding.BegrunnelseType.SkjønnsfastsattSykepengegrunnlagKonklusjon,
                            begrunnelse = "Konklusjon",
                        ),
                    ),
                beslutter = beslutterIdentOgNavn,
                saksbehandler = saksbehandlerIdentOgNavn,
                automatiskFattet = false,
                dekning = null,
                forsikringsvurderingId = null,
                utbetalingsdager = listOf(mapOf("utbetaling" to "medFriStruktur")),
            )
        utgåendeMeldingerMediator.hendelse(infotrygd)
        utgåendeMeldingerMediator.publiserOppsamledeMeldinger(personmelding, publiserer)
        val eventer = testRapid.inspektør.meldinger()

        assertEquals(1, eventer.size)

        val event = eventer.first()

        assertEquals("vedtak_fattet", event["@event_name"].asString())
        assertEquals(FØDSELSNUMMER, event["fødselsnummer"].asString())
        assertEquals(AKTØRID, event["aktørId"].asString())
        assertEquals(vedtaksperiodeId.toString(), event["vedtaksperiodeId"].asString())
        assertEquals(ORGANISASJONSNUMMER, event["organisasjonsnummer"].asString())
        assertEquals(fom, event["fom"].asLocalDate())
        assertEquals(tom, event["tom"].asLocalDate())
        assertEquals(skjæringstidspunkt, event["skjæringstidspunkt"].asLocalDate())
        assertEquals(hendelser, event["hendelser"].toList().map { UUID.fromString(it.asString()) })
        assertEquals("10000.0", event["sykepengegrunnlag"].asString())
        assertEquals(vedtakFattetTidspunkt, event["vedtakFattetTidspunkt"].asLocalDateTime())
        assertEquals(utbetalingId.toString(), event["utbetalingId"].asString())
        assertEquals("EtterSkjønn", event["sykepengegrunnlagsfakta"]["fastsatt"].asString())
        assertEquals("10000.0", event["sykepengegrunnlagsfakta"]["omregnetÅrsinntekt"].asString())
        assertEquals("12000.0", event["sykepengegrunnlagsfakta"]["innrapportertÅrsinntekt"].asString())
        assertEquals("30.0", event["sykepengegrunnlagsfakta"]["avviksprosent"].asString())
        assertEquals("711720.0", event["sykepengegrunnlagsfakta"]["6G"].asString())
        assertEquals(emptyList<String>(), objectMapper.convertValue(event["sykepengegrunnlagsfakta"]["tags"]))
        assertEquals(1, event["sykepengegrunnlagsfakta"]["arbeidsgivere"].size())
        assertEquals(ORGANISASJONSNUMMER, event["sykepengegrunnlagsfakta"]["arbeidsgivere"][0]["arbeidsgiver"].asString())
        assertEquals("10000.0", event["sykepengegrunnlagsfakta"]["arbeidsgivere"][0]["omregnetÅrsinntekt"].asString())
        assertEquals("12000.0", event["sykepengegrunnlagsfakta"]["arbeidsgivere"][0]["innrapportertÅrsinntekt"].asString())
        assertEquals("13000.0", event["sykepengegrunnlagsfakta"]["arbeidsgivere"][0]["skjønnsfastsatt"].asString())
        assertEquals("13000.0", event["sykepengegrunnlagsfakta"]["skjønnsfastsatt"].asString())

        assertEquals(3, event["begrunnelser"].size())

        assertEquals("SkjønnsfastsattSykepengegrunnlagMal", event["begrunnelser"][0]["type"].asString())
        assertEquals("Mal", event["begrunnelser"][0]["begrunnelse"].asString())
        assertEquals(
            listOf(mapOf("fom" to fom, "tom" to tom)),
            objectMapper.convertValue<List<Map<String, LocalDate>>>(event["begrunnelser"][0]["perioder"]),
        )

        assertEquals("SkjønnsfastsattSykepengegrunnlagFritekst", event["begrunnelser"][1]["type"].asString())
        assertEquals("Fritekst", event["begrunnelser"][1]["begrunnelse"].asString())
        assertEquals(
            listOf(mapOf("fom" to fom, "tom" to tom)),
            objectMapper.convertValue<List<Map<String, LocalDate>>>(event["begrunnelser"][1]["perioder"]),
        )

        assertEquals("SkjønnsfastsattSykepengegrunnlagKonklusjon", event["begrunnelser"][2]["type"].asString())
        assertEquals("Konklusjon", event["begrunnelser"][2]["begrunnelse"].asString())
        assertEquals(
            listOf(mapOf("fom" to fom, "tom" to tom)),
            objectMapper.convertValue<List<Map<String, LocalDate>>>(event["begrunnelser"][2]["perioder"]),
        )

        assertEquals(1, event["tags"].size())
        assertEquals("IngenNyArbeidsgiverperiode", event["tags"].first().asString())
        assertEquals(saksbehandlerIdentOgNavn.ident, event["saksbehandler"]["ident"].asString())
        assertEqualsNavn(saksbehandlerIdentOgNavn.navn, event["saksbehandler"]["navn"].asString())
        assertEquals(beslutterIdentOgNavn.ident, event["beslutter"]["ident"].asString())
        assertEqualsNavn(beslutterIdentOgNavn.navn, event["beslutter"]["navn"].asString())
        assertEquals(false, event["automatiskFattet"].asBoolean())
    }

    @Test
    fun `vanlig vedtak sykepengegrunnlag fastsatt etter skjønn med avslag`() {
        val infotrygd =
            VedtakFattetMelding(
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØRID,
                organisasjonsnummer = ORGANISASJONSNUMMER,
                yrkesaktivitetstype = YRKESAKTIVITETSTYPE,
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = spleisBehandlingId,
                utbetalingId = utbetalingId,
                fom = fom,
                tom = tom,
                skjæringstidspunkt = skjæringstidspunkt,
                hendelser = hendelser,
                sykepengegrunnlag = BigDecimal("10000.0"),
                sykepengegrunnlagsfakta =
                    VedtakFattetMelding.FastsattEtterSkjønnSykepengegrunnlagsfakta(
                        omregnetÅrsinntektTotalt = BigDecimal("10000.0"),
                        seksG = BigDecimal("711720.0"),
                        avviksprosent = BigDecimal("30.0"),
                        innrapportertÅrsinntekt = BigDecimal("13000.0"),
                        tags = setOf(),
                        arbeidsgivere =
                            listOf(
                                VedtakFattetMelding.FastsattEtterSkjønnSykepengegrunnlagsfakta.Arbeidsgiver(
                                    organisasjonsnummer = ORGANISASJONSNUMMER,
                                    omregnetÅrsinntekt = BigDecimal("10000.0"),
                                    innrapportertÅrsinntekt = BigDecimal("13000.0"),
                                    skjønnsfastsatt = BigDecimal("13000.0"),
                                ),
                            ),
                        skjønnsfastsatt = BigDecimal("13000.0"),
                        skjønnsfastsettingtype = VedtakFattetMelding.Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT,
                        skjønnsfastsettingsårsak = VedtakFattetMelding.Skjønnsfastsettingsårsak.ANDRE_AVSNITT,
                    ),
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                tags = setOf("IngenNyArbeidsgiverperiode"),
                begrunnelser =
                    listOf(
                        VedtakFattetMelding.Begrunnelse(
                            type = VedtakFattetMelding.BegrunnelseType.SkjønnsfastsattSykepengegrunnlagMal,
                            begrunnelse = "Mal",
                        ),
                        VedtakFattetMelding.Begrunnelse(
                            type = VedtakFattetMelding.BegrunnelseType.SkjønnsfastsattSykepengegrunnlagFritekst,
                            begrunnelse = "Fritekst",
                        ),
                        VedtakFattetMelding.Begrunnelse(
                            type = VedtakFattetMelding.BegrunnelseType.SkjønnsfastsattSykepengegrunnlagKonklusjon,
                            begrunnelse = "Konklusjon",
                        ),
                        VedtakFattetMelding.Begrunnelse(
                            type = VedtakFattetMelding.BegrunnelseType.Avslag,
                            begrunnelse = "En individuell begrunnelse",
                        ),
                    ),
                beslutter = beslutterIdentOgNavn,
                saksbehandler = saksbehandlerIdentOgNavn,
                automatiskFattet = false,
                dekning = null,
                forsikringsvurderingId = null,
                utbetalingsdager = listOf(mapOf("utbetaling" to "medFriStruktur")),
            )
        utgåendeMeldingerMediator.hendelse(infotrygd)
        utgåendeMeldingerMediator.publiserOppsamledeMeldinger(personmelding, publiserer)
        val eventer = testRapid.inspektør.meldinger()

        assertEquals(1, eventer.size)

        val event = eventer.first()

        assertEquals("vedtak_fattet", event["@event_name"].asString())
        assertEquals(FØDSELSNUMMER, event["fødselsnummer"].asString())
        assertEquals(AKTØRID, event["aktørId"].asString())
        assertEquals(vedtaksperiodeId.toString(), event["vedtaksperiodeId"].asString())
        assertEquals(ORGANISASJONSNUMMER, event["organisasjonsnummer"].asString())
        assertEquals(fom, event["fom"].asLocalDate())
        assertEquals(tom, event["tom"].asLocalDate())
        assertEquals(skjæringstidspunkt, event["skjæringstidspunkt"].asLocalDate())
        assertEquals(hendelser, event["hendelser"].toList().map { UUID.fromString(it.asString()) })
        assertEquals("10000.0", event["sykepengegrunnlag"].asString())
        assertEquals(vedtakFattetTidspunkt, event["vedtakFattetTidspunkt"].asLocalDateTime())
        assertEquals(utbetalingId.toString(), event["utbetalingId"].asString())
        assertEquals("EtterSkjønn", event["sykepengegrunnlagsfakta"]["fastsatt"].asString())
        assertEquals("10000.0", event["sykepengegrunnlagsfakta"]["omregnetÅrsinntekt"].asString())
        assertEquals("13000.0", event["sykepengegrunnlagsfakta"]["innrapportertÅrsinntekt"].asString())
        assertEquals("30.0", event["sykepengegrunnlagsfakta"]["avviksprosent"].asString())
        assertEquals("711720.0", event["sykepengegrunnlagsfakta"]["6G"].asString())
        assertEquals(emptyList<String>(), objectMapper.convertValue(event["sykepengegrunnlagsfakta"]["tags"]))
        assertEquals(1, event["sykepengegrunnlagsfakta"]["arbeidsgivere"].size())
        assertEquals(ORGANISASJONSNUMMER, event["sykepengegrunnlagsfakta"]["arbeidsgivere"][0]["arbeidsgiver"].asString())
        assertEquals("10000.0", event["sykepengegrunnlagsfakta"]["arbeidsgivere"][0]["omregnetÅrsinntekt"].asString())
        assertEquals("13000.0", event["sykepengegrunnlagsfakta"]["arbeidsgivere"][0]["innrapportertÅrsinntekt"].asString())
        assertEquals("13000.0", event["sykepengegrunnlagsfakta"]["arbeidsgivere"][0]["skjønnsfastsatt"].asString())
        assertEquals("13000.0", event["sykepengegrunnlagsfakta"]["skjønnsfastsatt"].asString())

        assertEquals(4, event["begrunnelser"].size())

        assertEquals("SkjønnsfastsattSykepengegrunnlagMal", event["begrunnelser"][0]["type"].asString())
        assertEquals("Mal", event["begrunnelser"][0]["begrunnelse"].asString())
        assertEquals(
            listOf(mapOf("fom" to fom, "tom" to tom)),
            objectMapper.convertValue<List<Map<String, LocalDate>>>(event["begrunnelser"][0]["perioder"]),
        )

        assertEquals("SkjønnsfastsattSykepengegrunnlagFritekst", event["begrunnelser"][1]["type"].asString())
        assertEquals("Fritekst", event["begrunnelser"][1]["begrunnelse"].asString())
        assertEquals(
            listOf(mapOf("fom" to fom, "tom" to tom)),
            objectMapper.convertValue<List<Map<String, LocalDate>>>(event["begrunnelser"][1]["perioder"]),
        )

        assertEquals("SkjønnsfastsattSykepengegrunnlagKonklusjon", event["begrunnelser"][2]["type"].asString())
        assertEquals("Konklusjon", event["begrunnelser"][2]["begrunnelse"].asString())
        assertEquals(
            listOf(mapOf("fom" to fom, "tom" to tom)),
            objectMapper.convertValue<List<Map<String, LocalDate>>>(event["begrunnelser"][2]["perioder"]),
        )

        assertEquals("Avslag", event["begrunnelser"][3]["type"].asString())
        assertEquals("En individuell begrunnelse", event["begrunnelser"][3]["begrunnelse"].asString())
        assertEquals(
            listOf(mapOf("fom" to fom, "tom" to tom)),
            objectMapper.convertValue<List<Map<String, LocalDate>>>(event["begrunnelser"][3]["perioder"]),
        )

        assertEquals(1, event["tags"].size())
        assertEquals("IngenNyArbeidsgiverperiode", event["tags"].first().asString())
        assertEquals(saksbehandlerIdentOgNavn.ident, event["saksbehandler"]["ident"].asString())
        assertEqualsNavn(saksbehandlerIdentOgNavn.navn, event["saksbehandler"]["navn"].asString())
        assertEquals(beslutterIdentOgNavn.ident, event["beslutter"]["ident"].asString())
        assertEqualsNavn(beslutterIdentOgNavn.navn, event["beslutter"]["navn"].asString())
        assertEquals(false, event["automatiskFattet"].asBoolean())
    }

    private fun assertEqualsNavn(
        expected: String,
        actual: String,
    ) {
        assertEquals(normaliserNavn(expected), normaliserNavn(actual))
    }

    private fun normaliserNavn(navn: String): String {
        val deler = navn.split(",", limit = 2)
        if (deler.size != 2) return navn

        val etternavn = deler[0].trim()
        val fornavnOgMellomnavn = deler[1].trim()

        return "$fornavnOgMellomnavn $etternavn"
    }
}
