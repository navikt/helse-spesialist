package no.nav.helse.spesialist.kafka.rivers

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import no.nav.helse.spesialist.application.testing.assertJsonEquals
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.Vedtak
import no.nav.helse.spesialist.domain.VedtakBegrunnelse
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.lagAvviksvurderingMedEnArbeidsgiver
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagSkjønnsfastsattSykepengegrunnlag
import no.nav.helse.spesialist.domain.testfixtures.lagVedtakBegrunnelse
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.kafka.IntegrationTestFixture
import no.nav.helse.spesialist.kafka.TestRapidHelpers.publiserteMeldingerUtenGenererteFelter
import no.nav.helse.spesialist.kafka.testfixtures.Testmeldingfabrikk.godkjenningsbehovFastsattEtterHovedregel
import no.nav.helse.spesialist.kafka.testfixtures.Testmeldingfabrikk.lagGodkjenningsbehov
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class AvsluttetMedVedtakRiverArbeidstakerIntegrationTest {
    private val testRapid = TestRapid()
    private val integrationTestFixture = IntegrationTestFixture(testRapid)
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    private val hendelser = listOf(UUID.randomUUID(), UUID.randomUUID())
    private val vedtakFattetTidspunkt = LocalDateTime.now()
    private val seksG = BigDecimal("666666.66")

    private lateinit var utfall: Utfall
    private lateinit var omregnetÅrsinntekt: BigDecimal
    private lateinit var innrapportertÅrsinntekt: BigDecimal
    private lateinit var behandlingTags: Set<String>

    @Test
    fun `fastsatt etter hovedregel`() {
        // Given:
        this.utfall = Utfall.INNVILGELSE
        this.omregnetÅrsinntekt = BigDecimal("600000.00")
        this.innrapportertÅrsinntekt = BigDecimal("660000.00")
        this.behandlingTags = setOf("Behandling tag 1", "Behandling tag 2")
        setup()
        sessionContext.vedtakRepository.lagre(Vedtak.automatisk(behandling.spleisBehandlingId!!))
        initGodkjenningsbehov()

        // When:
        testRapid.sendTestMessage(fastsattEtterHovedregelMelding(sykepengegrunnlag = omregnetÅrsinntekt))

        // Then:
        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(1, meldinger.size)
        assertEquals(person.id.value, meldinger.single().key)
        val actualJsonNode = meldinger.single().json

        @Language("JSON")
        val expectedJson =
            """
            {
              "@event_name": "vedtak_fattet",
              "fødselsnummer": "${person.id.value}",
              "aktørId": "${person.aktørId}",
              "yrkesaktivitetstype" : "ARBEIDSTAKER",
              "vedtaksperiodeId": "${vedtaksperiode.id.value}",
              "behandlingId": "${behandling.spleisBehandlingId?.value}",
              "organisasjonsnummer": "${vedtaksperiode.organisasjonsnummer}",
              "fom": "${behandling.fom}",
              "tom": "${behandling.tom}",
              "skjæringstidspunkt": "${behandling.skjæringstidspunkt}",
              "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlag": $omregnetÅrsinntekt,
              "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
              "utbetalingId": "${behandling.utbetalingId?.value}",
              "tags": [ ${behandling.tags.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlagsfakta": {
                "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                "innrapportertÅrsinntekt": $innrapportertÅrsinntekt,
                "avviksprosent": ${avviksvurdering.avviksprosent},
                "6G": $seksG,
                "tags": [],
                "arbeidsgivere": [
                  {
                    "arbeidsgiver": "${vedtaksperiode.organisasjonsnummer}",
                    "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                    "innrapportertÅrsinntekt": $innrapportertÅrsinntekt
                  }
                ],
                "fastsatt": "EtterHovedregel"
              },
              "begrunnelser": [
                {
                  "type" : "Innvilgelse",
                  "begrunnelse" : "${vedtakBegrunnelse.tekst}",
                  "perioder" : [
                    {
                      "fom" : "${behandling.fom}",
                      "tom" : "${behandling.tom}"
                    }
                  ]
                }
              ],
              "automatiskFattet": true
            }
            """.trimIndent()
        assertJsonEquals(expectedJson, actualJsonNode)
    }

    @Test
    fun `fastsatt etter hovedregel uten godkjenningsbehov`() {
        // Given:
        this.utfall = Utfall.INNVILGELSE
        this.omregnetÅrsinntekt = BigDecimal("600000.00")
        this.innrapportertÅrsinntekt = BigDecimal("660000.00")
        this.behandlingTags = setOf("Behandling tag 1", "Behandling tag 2")
        setup()
        sessionContext.vedtakRepository.lagre(Vedtak.automatisk(behandling.spleisBehandlingId!!))

        // When:
        testRapid.sendTestMessage(fastsattEtterHovedregelMelding(sykepengegrunnlag = omregnetÅrsinntekt))

        // Then:
        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(1, meldinger.size)
        assertEquals(person.id.value, meldinger.single().key)
        val actualJsonNode = meldinger.single().json

        @Language("JSON")
        val expectedJson =
            """
            {
              "@event_name": "vedtak_fattet",
              "fødselsnummer": "${person.id.value}",
              "aktørId": "${person.aktørId}",
              "yrkesaktivitetstype" : "ARBEIDSTAKER",
              "vedtaksperiodeId": "${vedtaksperiode.id.value}",
              "behandlingId": "${behandling.spleisBehandlingId?.value}",
              "organisasjonsnummer": "${vedtaksperiode.organisasjonsnummer}",
              "fom": "${behandling.fom}",
              "tom": "${behandling.tom}",
              "skjæringstidspunkt": "${behandling.skjæringstidspunkt}",
              "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlag": $omregnetÅrsinntekt,
              "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
              "utbetalingId": "${behandling.utbetalingId?.value}",
              "tags": [ ${behandling.tags.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlagsfakta": {
                "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                "innrapportertÅrsinntekt": $innrapportertÅrsinntekt,
                "avviksprosent": ${avviksvurdering.avviksprosent},
                "6G": $seksG,
                "tags": [],
                "arbeidsgivere": [
                  {
                    "arbeidsgiver": "${vedtaksperiode.organisasjonsnummer}",
                    "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                    "innrapportertÅrsinntekt": $innrapportertÅrsinntekt
                  }
                ],
                "fastsatt": "EtterHovedregel"
              },
              "begrunnelser": [
                {
                  "type" : "Innvilgelse",
                  "begrunnelse" : "${vedtakBegrunnelse.tekst}",
                  "perioder" : [
                    {
                      "fom" : "${behandling.fom}",
                      "tom" : "${behandling.tom}"
                    }
                  ]
                }
              ],
              "automatiskFattet": true
            }
            """.trimIndent()
        assertJsonEquals(expectedJson, actualJsonNode)
    }

    @Test
    fun `fastsatt etter hovedregel - delvis innvilgelse`() {
        // Given:
        this.utfall = Utfall.DELVIS_INNVILGELSE
        this.omregnetÅrsinntekt = BigDecimal("600000.00")
        this.innrapportertÅrsinntekt = BigDecimal("660000.00")
        this.behandlingTags = setOf("Behandling tag 1", "Behandling tag 2")
        setup()
        sessionContext.vedtakRepository.lagre(Vedtak.automatisk(behandling.spleisBehandlingId!!))
        initGodkjenningsbehov()

        // When:
        testRapid.sendTestMessage(fastsattEtterHovedregelMelding(sykepengegrunnlag = omregnetÅrsinntekt))

        // Then:
        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(1, meldinger.size)
        assertEquals(person.id.value, meldinger.single().key)
        val actualJsonNode = meldinger.single().json

        @Language("JSON")
        val expectedJson =
            """
            {
              "@event_name": "vedtak_fattet",
              "fødselsnummer": "${person.id.value}",
              "aktørId": "${person.aktørId}",
              "yrkesaktivitetstype" : "ARBEIDSTAKER",
              "vedtaksperiodeId": "${vedtaksperiode.id.value}",
              "behandlingId": "${behandling.spleisBehandlingId?.value}",
              "organisasjonsnummer": "${vedtaksperiode.organisasjonsnummer}",
              "fom": "${behandling.fom}",
              "tom": "${behandling.tom}",
              "skjæringstidspunkt": "${behandling.skjæringstidspunkt}",
              "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlag": $omregnetÅrsinntekt,
              "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
              "utbetalingId": "${behandling.utbetalingId?.value}",
              "tags": [ ${behandling.tags.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlagsfakta": {
                "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                "innrapportertÅrsinntekt": $innrapportertÅrsinntekt,
                "avviksprosent": ${avviksvurdering.avviksprosent},
                "6G": $seksG,
                "tags": [],
                "arbeidsgivere": [
                  {
                    "arbeidsgiver": "${vedtaksperiode.organisasjonsnummer}",
                    "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                    "innrapportertÅrsinntekt": $innrapportertÅrsinntekt
                  }
                ],
                "fastsatt": "EtterHovedregel"
              },
              "begrunnelser": [
                {
                  "type" : "DelvisInnvilgelse",
                  "begrunnelse" : "${vedtakBegrunnelse.tekst}",
                  "perioder" : [
                    {
                      "fom" : "${behandling.fom}",
                      "tom" : "${behandling.tom}"
                    }
                  ]
                }
              ],
              "automatiskFattet": true
            }
            """.trimIndent()
        assertJsonEquals(expectedJson, actualJsonNode)
    }

    @Test
    fun `fastsatt etter hovedregel begrenset til 6G`() {
        // Given:
        this.utfall = Utfall.INNVILGELSE
        this.omregnetÅrsinntekt = BigDecimal("900000.00")
        this.innrapportertÅrsinntekt = BigDecimal("990000.00")
        this.behandlingTags = setOf("Behandling tag 1", "Behandling tag 2", "6GBegrenset")
        setup()
        sessionContext.vedtakRepository.lagre(Vedtak.automatisk(behandling.spleisBehandlingId!!))
        initGodkjenningsbehov()

        // When:
        testRapid.sendTestMessage(fastsattEtterHovedregelMelding(sykepengegrunnlag = seksG))

        // Then:
        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(1, meldinger.size)
        assertEquals(person.id.value, meldinger.single().key)
        val actualJsonNode = meldinger.single().json

        @Language("JSON")
        val expectedJson =
            """
            {
              "@event_name": "vedtak_fattet",
              "fødselsnummer": "${person.id.value}",
              "aktørId": "${person.aktørId}",
              "yrkesaktivitetstype" : "ARBEIDSTAKER",
              "vedtaksperiodeId": "${vedtaksperiode.id.value}",
              "behandlingId": "${behandling.spleisBehandlingId?.value}",
              "organisasjonsnummer": "${vedtaksperiode.organisasjonsnummer}",
              "fom": "${behandling.fom}",
              "tom": "${behandling.tom}",
              "skjæringstidspunkt": "${behandling.skjæringstidspunkt}",
              "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlag": $seksG,
              "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
              "utbetalingId": "${behandling.utbetalingId?.value}",
              "tags": [ ${behandling.tags.minus("6GBegrenset").joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlagsfakta": {
                "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                "innrapportertÅrsinntekt": $innrapportertÅrsinntekt,
                "avviksprosent": ${avviksvurdering.avviksprosent},
                "6G": $seksG,
                "tags": [ "6GBegrenset" ],
                "arbeidsgivere": [
                  {
                    "arbeidsgiver": "${vedtaksperiode.organisasjonsnummer}",
                    "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                    "innrapportertÅrsinntekt": $innrapportertÅrsinntekt
                  }
                ],
                "fastsatt": "EtterHovedregel"
              },
              "begrunnelser": [
                {
                  "type" : "Innvilgelse",
                  "begrunnelse" : "${vedtakBegrunnelse.tekst}",
                  "perioder" : [
                    {
                      "fom" : "${behandling.fom}",
                      "tom" : "${behandling.tom}"
                    }
                  ]
                }
              ],
              "automatiskFattet": true
            }
            """.trimIndent()
        assertJsonEquals(expectedJson, actualJsonNode)
    }

    @Test
    fun `fastsatt etter skjønn`() {
        // Given:
        this.utfall = Utfall.INNVILGELSE
        this.omregnetÅrsinntekt = BigDecimal("800000.00")
        this.innrapportertÅrsinntekt = BigDecimal("1200000.00")
        this.behandlingTags = setOf("Behandling tag 1", "Behandling tag 2")
        setup()
        sessionContext.vedtakRepository.lagre(Vedtak.automatisk(behandling.spleisBehandlingId!!))
        initGodkjenningsbehov()

        val skjønnsfastsattBeløp = BigDecimal("650000.00")
        val skjønnsfastsettelse = setupSkjønnsfastsettelse(skjønnsfastsattBeløp)

        // When:
        testRapid.sendTestMessage(fastsattEtterSkjønnMelding(skjønnsfastsattSykepengegrunnlag = skjønnsfastsattBeløp))

        // Then:
        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(1, meldinger.size)
        assertEquals(person.id.value, meldinger.single().key)
        val actualJsonNode = meldinger.single().json

        @Language("JSON")
        val expectedJson =
            """
            {
              "@event_name": "vedtak_fattet",
              "fødselsnummer": "${person.id.value}",
              "aktørId": "${person.aktørId}",
              "yrkesaktivitetstype" : "ARBEIDSTAKER",
              "vedtaksperiodeId": "${vedtaksperiode.id.value}",
              "behandlingId": "${behandling.spleisBehandlingId?.value}",
              "organisasjonsnummer": "${vedtaksperiode.organisasjonsnummer}",
              "fom": "${behandling.fom}",
              "tom": "${behandling.tom}",
              "skjæringstidspunkt": "${behandling.skjæringstidspunkt}",
              "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlag": $skjønnsfastsattBeløp,
              "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
              "utbetalingId": "${behandling.utbetalingId?.value}",
              "tags": [ ${behandling.tags.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlagsfakta": {
                "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                "innrapportertÅrsinntekt": $innrapportertÅrsinntekt,
                "avviksprosent": ${avviksvurdering.avviksprosent},
                "6G": $seksG,
                "tags": [],
                "arbeidsgivere": [
                  {
                    "arbeidsgiver": "${vedtaksperiode.organisasjonsnummer}",
                    "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                    "innrapportertÅrsinntekt": $innrapportertÅrsinntekt,
                    "skjønnsfastsatt": $skjønnsfastsattBeløp
                  }
                ],
                "fastsatt": "EtterSkjønn",
                "skjønnsfastsettingtype": "ANNET",
                "skjønnsfastsettingårsak": "ANDRE_AVSNITT",
                "skjønnsfastsatt": $skjønnsfastsattBeløp
              },
              "begrunnelser": [
                {
                  "type": "SkjønnsfastsattSykepengegrunnlagMal",
                  "begrunnelse": "${skjønnsfastsettelse.arbeidsgivere.first().begrunnelseMal}",
                  "perioder": [
                    {
                      "fom": "${behandling.fom}",
                      "tom": "${behandling.tom}"
                    }
                  ]
                },
                {
                  "type": "SkjønnsfastsattSykepengegrunnlagFritekst",
                  "begrunnelse": "${skjønnsfastsettelse.arbeidsgivere.first().begrunnelseFritekst}",
                  "perioder": [
                    {
                      "fom": "${behandling.fom}",
                      "tom": "${behandling.tom}"
                    }
                  ]
                },
                {
                  "type": "SkjønnsfastsattSykepengegrunnlagKonklusjon",
                  "begrunnelse": "${skjønnsfastsettelse.arbeidsgivere.first().begrunnelseKonklusjon}",
                  "perioder": [
                    {
                      "fom": "${behandling.fom}",
                      "tom": "${behandling.tom}"
                    }
                  ]
                },
                {
                  "type" : "Innvilgelse",
                  "begrunnelse" : "${vedtakBegrunnelse.tekst}",
                  "perioder" : [
                    {
                      "fom" : "${behandling.fom}",
                      "tom" : "${behandling.tom}"
                    }
                  ]
                }
              ],
              "automatiskFattet": true
            }
            """.trimIndent()
        assertJsonEquals(expectedJson, actualJsonNode)
    }

    @Test
    fun `fastsatt etter skjønn - delvis innvilgelse`() {
        // Given:
        this.utfall = Utfall.DELVIS_INNVILGELSE
        this.omregnetÅrsinntekt = BigDecimal("800000.00")
        this.innrapportertÅrsinntekt = BigDecimal("1200000.00")
        this.behandlingTags = setOf("Behandling tag 1", "Behandling tag 2")
        setup()
        sessionContext.vedtakRepository.lagre(Vedtak.automatisk(behandling.spleisBehandlingId!!))
        initGodkjenningsbehov()

        val skjønnsfastsattBeløp = BigDecimal("650000.00")
        val skjønnsfastsettelse = setupSkjønnsfastsettelse(skjønnsfastsattBeløp)

        // When:
        testRapid.sendTestMessage(fastsattEtterSkjønnMelding(skjønnsfastsattSykepengegrunnlag = skjønnsfastsattBeløp))

        // Then:
        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(1, meldinger.size)
        assertEquals(person.id.value, meldinger.single().key)
        val actualJsonNode = meldinger.single().json

        @Language("JSON")
        val expectedJson =
            """
            {
              "@event_name": "vedtak_fattet",
              "fødselsnummer": "${person.id.value}",
              "aktørId": "${person.aktørId}",
              "yrkesaktivitetstype" : "ARBEIDSTAKER",
              "vedtaksperiodeId": "${vedtaksperiode.id.value}",
              "behandlingId": "${behandling.spleisBehandlingId?.value}",
              "organisasjonsnummer": "${vedtaksperiode.organisasjonsnummer}",
              "fom": "${behandling.fom}",
              "tom": "${behandling.tom}",
              "skjæringstidspunkt": "${behandling.skjæringstidspunkt}",
              "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlag": $skjønnsfastsattBeløp,
              "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
              "utbetalingId": "${behandling.utbetalingId?.value}",
              "tags": [ ${behandling.tags.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlagsfakta": {
                "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                "innrapportertÅrsinntekt": $innrapportertÅrsinntekt,
                "avviksprosent": ${avviksvurdering.avviksprosent},
                "6G": $seksG,
                "tags": [],
                "arbeidsgivere": [
                  {
                    "arbeidsgiver": "${vedtaksperiode.organisasjonsnummer}",
                    "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                    "innrapportertÅrsinntekt": $innrapportertÅrsinntekt,
                    "skjønnsfastsatt": $skjønnsfastsattBeløp
                  }
                ],
                "fastsatt": "EtterSkjønn",
                "skjønnsfastsettingtype": "ANNET",
                "skjønnsfastsettingårsak": "ANDRE_AVSNITT",
                "skjønnsfastsatt": $skjønnsfastsattBeløp
              },
              "begrunnelser": [
                {
                  "type": "SkjønnsfastsattSykepengegrunnlagMal",
                  "begrunnelse": "${skjønnsfastsettelse.arbeidsgivere.first().begrunnelseMal}",
                  "perioder": [
                    {
                      "fom": "${behandling.fom}",
                      "tom": "${behandling.tom}"
                    }
                  ]
                },
                {
                  "type": "SkjønnsfastsattSykepengegrunnlagFritekst",
                  "begrunnelse": "${skjønnsfastsettelse.arbeidsgivere.first().begrunnelseFritekst}",
                  "perioder": [
                    {
                      "fom": "${behandling.fom}",
                      "tom": "${behandling.tom}"
                    }
                  ]
                },
                {
                  "type": "SkjønnsfastsattSykepengegrunnlagKonklusjon",
                  "begrunnelse": "${skjønnsfastsettelse.arbeidsgivere.first().begrunnelseKonklusjon}",
                  "perioder": [
                    {
                      "fom": "${behandling.fom}",
                      "tom": "${behandling.tom}"
                    }
                  ]
                },
                {
                  "type" : "DelvisInnvilgelse",
                  "begrunnelse" : "${vedtakBegrunnelse.tekst}",
                  "perioder" : [
                    {
                      "fom" : "${behandling.fom}",
                      "tom" : "${behandling.tom}"
                    }
                  ]
                }
              ],
              "automatiskFattet": true
            }
            """.trimIndent()
        assertJsonEquals(expectedJson, actualJsonNode)
    }

    @Test
    fun `fastsatt i infotrygd`() {
        // Given:
        this.utfall = Utfall.INNVILGELSE
        this.omregnetÅrsinntekt = BigDecimal("600000.00")
        this.innrapportertÅrsinntekt = BigDecimal("660000.00")
        this.behandlingTags = setOf("Behandling tag 1", "Behandling tag 2")
        setup()
        sessionContext.vedtakRepository.lagre(Vedtak.automatisk(behandling.spleisBehandlingId!!))
        initGodkjenningsbehov()

        // When:
        testRapid.sendTestMessage(fastsattIInfotrygdMelding(sykepengegrunnlag = omregnetÅrsinntekt))

        // Then:
        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(1, meldinger.size)
        assertEquals(person.id.value, meldinger.single().key)
        val actualJsonNode = meldinger.single().json

        @Language("JSON")
        val expectedJson =
            """
            {
              "@event_name": "vedtak_fattet",
              "fødselsnummer": "${person.id.value}",
              "aktørId": "${person.aktørId}",
              "yrkesaktivitetstype" : "ARBEIDSTAKER",
              "vedtaksperiodeId": "${vedtaksperiode.id.value}",
              "behandlingId": "${behandling.spleisBehandlingId?.value}",
              "organisasjonsnummer": "${vedtaksperiode.organisasjonsnummer}",
              "fom": "${behandling.fom}",
              "tom": "${behandling.tom}",
              "skjæringstidspunkt": "${behandling.skjæringstidspunkt}",
              "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlag": $omregnetÅrsinntekt,
              "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
              "utbetalingId": "${behandling.utbetalingId?.value}",
              "tags": [ ${behandling.tags.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlagsfakta": {
                "fastsatt": "IInfotrygd",
                "omregnetÅrsinntekt": $omregnetÅrsinntekt
              },
              "begrunnelser": [
                {
                  "type" : "Innvilgelse",
                  "begrunnelse" : "${vedtakBegrunnelse.tekst}",
                  "perioder" : [
                    {
                      "fom" : "${behandling.fom}",
                      "tom" : "${behandling.tom}"
                    }
                  ]
                }
              ],
              "automatiskFattet": true
            }
            """.trimIndent()
        assertJsonEquals(expectedJson, actualJsonNode)
    }

    @Test
    fun `vedtak fattet manuelt med totrinnskontroll`() {
        // Given:
        this.utfall = Utfall.INNVILGELSE
        this.omregnetÅrsinntekt = BigDecimal("600000.00")
        this.innrapportertÅrsinntekt = BigDecimal("660000.00")
        this.behandlingTags = setOf("Behandling tag 1", "Behandling tag 2")
        setup()
        val beslutter = lagSaksbehandler().also(sessionContext.saksbehandlerRepository::lagre)
        sessionContext.vedtakRepository.lagre(Vedtak.manueltMedTotrinnskontroll(behandling.spleisBehandlingId!!, saksbehandler.ident, beslutter.ident))
        initGodkjenningsbehov()

        // When:
        testRapid.sendTestMessage(fastsattEtterHovedregelMelding(sykepengegrunnlag = omregnetÅrsinntekt))

        // Then:
        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(1, meldinger.size)
        assertEquals(person.id.value, meldinger.single().key)
        val actualJsonNode = meldinger.single().json

        @Language("JSON")
        val expectedJson =
            """
            {
              "@event_name": "vedtak_fattet",
              "fødselsnummer": "${person.id.value}",
              "aktørId": "${person.aktørId}",
              "yrkesaktivitetstype" : "ARBEIDSTAKER",
              "vedtaksperiodeId": "${vedtaksperiode.id.value}",
              "behandlingId": "${behandling.spleisBehandlingId?.value}",
              "organisasjonsnummer": "${vedtaksperiode.organisasjonsnummer}",
              "fom": "${behandling.fom}",
              "tom": "${behandling.tom}",
              "skjæringstidspunkt": "${behandling.skjæringstidspunkt}",
              "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlag": $omregnetÅrsinntekt,
              "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
              "utbetalingId": "${behandling.utbetalingId?.value}",
              "tags": [ ${behandling.tags.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlagsfakta": {
                "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                "innrapportertÅrsinntekt": $innrapportertÅrsinntekt,
                "avviksprosent": ${avviksvurdering.avviksprosent},
                "6G": $seksG,
                "tags": [],
                "arbeidsgivere": [
                  {
                    "arbeidsgiver": "${vedtaksperiode.organisasjonsnummer}",
                    "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                    "innrapportertÅrsinntekt": $innrapportertÅrsinntekt
                  }
                ],
                "fastsatt": "EtterHovedregel"
              },
              "begrunnelser": [
                {
                  "type" : "Innvilgelse",
                  "begrunnelse" : "${vedtakBegrunnelse.tekst}",
                  "perioder" : [
                    {
                      "fom" : "${behandling.fom}",
                      "tom" : "${behandling.tom}"
                    }
                  ]
                }
              ],
              "saksbehandler" : {
                "ident": "${saksbehandler.ident.value}",
                "navn": "${normaliserNavn(saksbehandler.navn)}"
              },
              "beslutter" : {
                "ident": "${beslutter.ident.value}",
                "navn": "${normaliserNavn(beslutter.navn)}"
              },
              "automatiskFattet": false
            }
            """.trimIndent()
        assertJsonEquals(expectedJson, actualJsonNode)
    }

    @Test
    fun `vedtak fattet manuelt uten totrinnskontroll`() {
        // Given:
        this.utfall = Utfall.INNVILGELSE
        this.omregnetÅrsinntekt = BigDecimal("600000.00")
        this.innrapportertÅrsinntekt = BigDecimal("660000.00")
        this.behandlingTags = setOf("Behandling tag 1", "Behandling tag 2")
        setup()
        sessionContext.vedtakRepository.lagre(Vedtak.manueltUtenTotrinnskontroll(behandling.spleisBehandlingId!!, saksbehandler.ident))
        initGodkjenningsbehov()

        // When:
        testRapid.sendTestMessage(fastsattEtterHovedregelMelding(sykepengegrunnlag = omregnetÅrsinntekt))

        // Then:
        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(1, meldinger.size)
        assertEquals(person.id.value, meldinger.single().key)
        val actualJsonNode = meldinger.single().json

        @Language("JSON")
        val expectedJson =
            """
            {
              "@event_name": "vedtak_fattet",
              "fødselsnummer": "${person.id.value}",
              "aktørId": "${person.aktørId}",
              "yrkesaktivitetstype" : "ARBEIDSTAKER",
              "vedtaksperiodeId": "${vedtaksperiode.id.value}",
              "behandlingId": "${behandling.spleisBehandlingId?.value}",
              "organisasjonsnummer": "${vedtaksperiode.organisasjonsnummer}",
              "fom": "${behandling.fom}",
              "tom": "${behandling.tom}",
              "skjæringstidspunkt": "${behandling.skjæringstidspunkt}",
              "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlag": $omregnetÅrsinntekt,
              "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
              "utbetalingId": "${behandling.utbetalingId?.value}",
              "tags": [ ${behandling.tags.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlagsfakta": {
                "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                "innrapportertÅrsinntekt": $innrapportertÅrsinntekt,
                "avviksprosent": ${avviksvurdering.avviksprosent},
                "6G": $seksG,
                "tags": [],
                "arbeidsgivere": [
                  {
                    "arbeidsgiver": "${vedtaksperiode.organisasjonsnummer}",
                    "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                    "innrapportertÅrsinntekt": $innrapportertÅrsinntekt
                  }
                ],
                "fastsatt": "EtterHovedregel"
              },
              "begrunnelser": [
                {
                  "type" : "Innvilgelse",
                  "begrunnelse" : "${vedtakBegrunnelse.tekst}",
                  "perioder" : [
                    {
                      "fom" : "${behandling.fom}",
                      "tom" : "${behandling.tom}"
                    }
                  ]
                }
              ],
              "saksbehandler" : {
                "ident": "${saksbehandler.ident.value}",
                "navn": "${normaliserNavn(saksbehandler.navn)}"
              },
              "automatiskFattet": false
            }
            """.trimIndent()
        assertJsonEquals(expectedJson, actualJsonNode)
    }

    private lateinit var saksbehandler: Saksbehandler
    private lateinit var person: Person
    private lateinit var vedtaksperiode: Vedtaksperiode
    private lateinit var behandling: Behandling
    private lateinit var vedtakBegrunnelse: VedtakBegrunnelse
    private lateinit var avviksvurdering: Avviksvurdering

    private fun setup() {
        this.saksbehandler =
            lagSaksbehandler()
                .also(sessionContext.saksbehandlerRepository::lagre)

        this.person =
            lagPerson()
                .also(sessionContext.personRepository::lagre)

        this.vedtaksperiode =
            lagVedtaksperiode(identitetsnummer = person.id)
                .also(sessionContext.vedtaksperiodeRepository::lagre)

        this.behandling =
            lagBehandling(
                vedtaksperiodeId = vedtaksperiode.id,
                tags = behandlingTags,
                yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
            ).also(sessionContext.behandlingRepository::lagre)

        this.vedtakBegrunnelse =
            lagVedtakBegrunnelse(
                spleisBehandlingId = behandling.spleisBehandlingId!!,
                utfall = utfall,
                saksbehandlerOid = saksbehandler.id,
            ).also(sessionContext.vedtakBegrunnelseRepository::lagre)

        this.avviksvurdering =
            lagAvviksvurderingMedEnArbeidsgiver(
                identitetsnummer = person.id,
                organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                skjæringstidspunkt = behandling.skjæringstidspunkt,
                omregnetÅrsinntekt = omregnetÅrsinntekt,
                innrapportertÅrsinntekt = innrapportertÅrsinntekt,
            ).also(sessionContext.avviksvurderingRepository::lagre)
    }

    private fun setupSkjønnsfastsettelse(skjønnsfastsattBeløp: BigDecimal): SkjønnsfastsattSykepengegrunnlag =
        lagSkjønnsfastsattSykepengegrunnlag(
            saksbehandlerOid = saksbehandler.id,
            vedtaksperiodeId = vedtaksperiode.id,
            identitetsnummer = person.id,
            aktørId = person.aktørId,
            skjæringstidspunkt = behandling.skjæringstidspunkt,
            organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
            skjønnsfastsattBeløp = skjønnsfastsattBeløp,
            omregnetÅrsinntekt = omregnetÅrsinntekt,
        ).also {
            sessionContext.overstyringRepository.lagre(
                overstyringer = listOf(it),
                totrinnsvurderingId =
                    Totrinnsvurdering
                        .ny(person.id.value)
                        .also(sessionContext.totrinnsvurderingRepository::lagre)
                        .id(),
            )
        }

    private fun initGodkjenningsbehov() {
        val godkjenningsbehovId = UUID.randomUUID()
        val godkjenningsbehovJson =
            lagGodkjenningsbehov(
                id = godkjenningsbehovId,
                aktørId = person.aktørId,
                fødselsnummer = person.id.value,
                spleisBehandlingId = behandling.spleisBehandlingId!!.value,
                yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
                sykepengegrunnlagsfakta =
                    godkjenningsbehovFastsattEtterHovedregel(
                        arbeidsgivere =
                            listOf(
                                mapOf(
                                    "arbeidsgiver" to vedtaksperiode.organisasjonsnummer,
                                    "omregnetÅrsinntekt" to omregnetÅrsinntekt.toDouble(),
                                    "inntektskilde" to "Arbeidsgiver",
                                ),
                            ),
                    ),
            )
        sessionContext.meldingDao.godkjenningsbehov.add(Godkjenningsbehov.fraJson(godkjenningsbehovJson))
    }

    @Language("JSON")
    private fun fastsattEtterHovedregelMelding(sykepengegrunnlag: BigDecimal) =
        """
        {
          "@event_name": "avsluttet_med_vedtak",
          "organisasjonsnummer": "${vedtaksperiode.organisasjonsnummer}",
          "yrkesaktivitetstype": "ARBEIDSTAKER",
          "vedtaksperiodeId": "${vedtaksperiode.id.value}",
          "behandlingId": "${behandling.spleisBehandlingId?.value}",
          "fom": "${behandling.fom}",
          "tom": "${behandling.tom}",
          "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
          "skjæringstidspunkt": "${behandling.skjæringstidspunkt}",
          "sykepengegrunnlag": $sykepengegrunnlag,
          "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
          "utbetalingId": "${behandling.utbetalingId?.value}",
          "sykepengegrunnlagsfakta": {
            "fastsatt": "EtterHovedregel",
            "omregnetÅrsinntekt": $omregnetÅrsinntekt,
            "omregnetÅrsinntektTotalt": $omregnetÅrsinntekt,
            "sykepengegrunnlag": $sykepengegrunnlag,
            "6G": $seksG,
            "arbeidsgivere": [
              {
                "arbeidsgiver": "${vedtaksperiode.organisasjonsnummer}",
                "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                "inntektskilde": "Arbeidsgiver"
              }
            ]
          },
          "@id": "0c9012d8-75e3-4f5e-93d2-c91d4439489c",
          "@opprettet": "2025-08-06T07:20:35.789710579",
          "system_read_count": 1,
          "system_participating_services": [
            {
              "id": "0c9012d8-75e3-4f5e-93d2-c91d4439489c",
              "time": "2025-08-06T07:20:35.789710579",
              "service": "helse-spleis",
              "instance": "helse-spleis-7667685c79-g6r6c",
              "image": "europe-north1-docker.pkg.dev/nais-management-233d/tbd/helse-spleis-spleis:2025.08.05-11.25-116e25f"
            },
            {
              "id": "0c9012d8-75e3-4f5e-93d2-c91d4439489c",
              "time": "2025-08-06T07:20:35.959076958",
              "service": "spesialist",
              "instance": "spesialist-58c64b76cd-hbl8z",
              "image": "europe-north1-docker.pkg.dev/nais-management-233d/tbd/helse-spesialist:2025.08.05-08.17-593599a"
            }
          ],
          "fødselsnummer": "${person.id.value}",
          "@forårsaket_av": {
            "id": "37d42c5a-2f5c-4acf-88de-3e7f958730ad",
            "opprettet": "2025-08-06T07:20:35.756560675",
            "event_name": "behov",
            "behov": [
              "Utbetaling"
            ]
          }
        }
        """.trimIndent()

    @Language("JSON")
    private fun fastsattEtterSkjønnMelding(skjønnsfastsattSykepengegrunnlag: BigDecimal) =
        """
        {
          "@event_name": "avsluttet_med_vedtak",
          "organisasjonsnummer": "${vedtaksperiode.organisasjonsnummer}",
          "yrkesaktivitetstype": "ARBEIDSTAKER",
          "vedtaksperiodeId": "${vedtaksperiode.id.value}",
          "behandlingId": "${behandling.spleisBehandlingId?.value}",
          "fom": "${behandling.fom}",
          "tom": "${behandling.tom}",
          "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
          "skjæringstidspunkt": "${behandling.skjæringstidspunkt}",
          "sykepengegrunnlag": $skjønnsfastsattSykepengegrunnlag,
          "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
          "utbetalingId": "${behandling.utbetalingId?.value}",
          "sykepengegrunnlagsfakta": {
            "fastsatt": "EtterSkjønn",
            "omregnetÅrsinntekt": $omregnetÅrsinntekt,
            "omregnetÅrsinntektTotalt": $omregnetÅrsinntekt,
            "6G": $seksG,
            "arbeidsgivere": [
              {
                "arbeidsgiver": "${vedtaksperiode.organisasjonsnummer}",
                "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                "skjønnsfastsatt": $skjønnsfastsattSykepengegrunnlag,
                "inntektskilde": "Saksbehandler"
              }
            ],
            "skjønnsfastsatt": $skjønnsfastsattSykepengegrunnlag
          },
          "@id": "5a3603c9-8da2-49c0-8860-964e6f1f2eaf",
          "@opprettet": "2025-08-06T13:49:40.550321649",
          "system_read_count": 1,
          "system_participating_services": [
            {
              "id": "5a3603c9-8da2-49c0-8860-964e6f1f2eaf",
              "time": "2025-08-06T13:49:40.550321649",
              "service": "helse-spleis",
              "instance": "helse-spleis-5bfbccf45-crpkd",
              "image": "europe-north1-docker.pkg.dev/nais-management-233d/tbd/helse-spleis-spleis:2025.08.06-11.34-6700db9"
            },
            {
              "id": "5a3603c9-8da2-49c0-8860-964e6f1f2eaf",
              "time": "2025-08-06T13:49:40.736788486",
              "service": "spesialist",
              "instance": "spesialist-76c6f4cdd6-mztpr",
              "image": "europe-north1-docker.pkg.dev/nais-management-233d/tbd/helse-spesialist:2025.08.06-08.08-3ed030a"
            }
          ],
          "fødselsnummer": "${person.id.value}",
          "@forårsaket_av": {
            "id": "853ff18a-c206-4688-9ad9-399c1a55b339",
            "opprettet": "2025-08-06T13:49:40.342278012",
            "event_name": "behov",
            "behov": [
              "Utbetaling"
            ]
          }
        }
        """.trimIndent()

    @Language("JSON")
    private fun fastsattIInfotrygdMelding(sykepengegrunnlag: BigDecimal) =
        """
        {
          "@event_name": "avsluttet_med_vedtak",
          "organisasjonsnummer": "${vedtaksperiode.organisasjonsnummer}",
          "yrkesaktivitetstype": "ARBEIDSTAKER",
          "vedtaksperiodeId": "${vedtaksperiode.id.value}",
          "behandlingId": "${behandling.spleisBehandlingId?.value}",
          "fom": "${behandling.fom}",
          "tom": "${behandling.tom}",
          "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
          "skjæringstidspunkt": "${behandling.skjæringstidspunkt}",
          "sykepengegrunnlag": $sykepengegrunnlag,
          "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
          "utbetalingId": "${behandling.utbetalingId?.value}",
          "sykepengegrunnlagsfakta": {
            "fastsatt": "IInfotrygd",
            "omregnetÅrsinntektTotalt": $omregnetÅrsinntekt
          },
          "@id": "0c9012d8-75e3-4f5e-93d2-c91d4439489c",
          "@opprettet": "2025-08-06T07:20:35.789710579",
          "system_read_count": 1,
          "system_participating_services": [
            {
              "id": "0c9012d8-75e3-4f5e-93d2-c91d4439489c",
              "time": "2025-08-06T07:20:35.789710579",
              "service": "helse-spleis",
              "instance": "helse-spleis-7667685c79-g6r6c",
              "image": "europe-north1-docker.pkg.dev/nais-management-233d/tbd/helse-spleis-spleis:2025.08.05-11.25-116e25f"
            },
            {
              "id": "0c9012d8-75e3-4f5e-93d2-c91d4439489c",
              "time": "2025-08-06T07:20:35.959076958",
              "service": "spesialist",
              "instance": "spesialist-58c64b76cd-hbl8z",
              "image": "europe-north1-docker.pkg.dev/nais-management-233d/tbd/helse-spesialist:2025.08.05-08.17-593599a"
            }
          ],
          "fødselsnummer": "${person.id.value}",
          "@forårsaket_av": {
            "id": "37d42c5a-2f5c-4acf-88de-3e7f958730ad",
            "opprettet": "2025-08-06T07:20:35.756560675",
            "event_name": "behov",
            "behov": [
              "Utbetaling"
            ]
          }
        }
        """.trimIndent()

    private fun normaliserNavn(navn: String): String {
        val deler = navn.split(",", limit = 2)
        if (deler.size != 2) return navn

        val etternavn = deler[0].trim()
        val fornavnOgMellomnavn = deler[1].trim()

        return "$fornavnOgMellomnavn $etternavn"
    }
}
