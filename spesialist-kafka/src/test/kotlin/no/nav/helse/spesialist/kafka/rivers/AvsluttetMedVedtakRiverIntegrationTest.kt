package no.nav.helse.spesialist.kafka.rivers

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.modell.person.Person
import no.nav.helse.modell.person.vedtaksperiode.BehandlingDto
import no.nav.helse.modell.person.vedtaksperiode.TilstandDto
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlagDto
import no.nav.helse.modell.vedtak.SkjønnsfastsettingstypeDto
import no.nav.helse.modell.vedtak.SkjønnsfastsettingsårsakDto
import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.modell.vedtak.VedtakBegrunnelse
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import no.nav.helse.modell.vilkårsprøving.Beregningsgrunnlag
import no.nav.helse.modell.vilkårsprøving.InnrapportertInntekt
import no.nav.helse.modell.vilkårsprøving.Inntekt
import no.nav.helse.modell.vilkårsprøving.OmregnetÅrsinntekt
import no.nav.helse.modell.vilkårsprøving.Sammenligningsgrunnlag
import no.nav.helse.spesialist.domain.testfixtures.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.kafka.IntegrationTestFixture
import no.nav.helse.spesialist.kafka.TestRapidHelpers.publiserteMeldingerUtenGenererteFelter
import no.nav.helse.spesialist.kafka.objectMapper
import no.nav.helse.spesialist.kafka.testfixtures.Testmeldingfabrikk.godkjenningsbehovFastsattEtterHovedregel
import no.nav.helse.spesialist.kafka.testfixtures.Testmeldingfabrikk.godkjenningsbehovSelvstendigNæringsdrivende
import no.nav.helse.spesialist.kafka.testfixtures.Testmeldingfabrikk.lagGodkjenningsbehov
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.YearMonth
import java.util.UUID

class AvsluttetMedVedtakRiverIntegrationTest {
    private val testRapid = TestRapid()
    private val integrationTestFixture = IntegrationTestFixture(testRapid)
    private val legacyPersonRepository = integrationTestFixture.sessionFactory.sessionContext.legacyPersonRepository
    private val vedtakDao = integrationTestFixture.sessionFactory.sessionContext.vedtakDao
    private val meldingDao = integrationTestFixture.sessionFactory.sessionContext.meldingDao

    private val organisasjonsnummer = lagOrganisasjonsnummer()
    private val vedtaksperiodeId = UUID.randomUUID()
    private val spleisBehandlingId = UUID.randomUUID()
    private val fom = LocalDate.parse("2024-10-01")
    private val tom = LocalDate.parse("2024-10-31")
    private val hendelser = listOf(UUID.randomUUID(), UUID.randomUUID())
    private val skjæringstidspunkt = fom
    private val fødselsnummer = lagFødselsnummer()
    private val vedtakFattetTidspunkt = LocalDateTime.now()
    private val utbetalingId = UUID.randomUUID()

    private val behandlingId = UUID.randomUUID()
    private val aktørId = lagAktørId()

    @Test
    fun `fastsatt etter skjønn`() {
        // Given:
        val skjønnsfastsattBeløp = BigDecimal("650000.00")
        val omregnetÅrsinntekt = BigDecimal("800000.00")
        val seksG = BigDecimal("666666.66")
        val innrapportertÅrsinntekt = BigDecimal("1200000.00")
        val avviksprosent = 50.0
        val behandlingTags = listOf("Behandling tag 1", "Behandling tag 2")
        val vedtakBegrunnelse = "Begrunnelse for innvilgelse"
        val skjønnsfastsettelseBegrunnelseFraMal = "Begrunnelse fra mal her"
        val skjønnsfastsettelseBegrunnelseFraFritekst = "Begrunnelse fra fritekst her"
        val skjønnsfastsettelseBegrunnelseFraKonklusjon = "Begrunnelse fra konklusjon her"
        initPersonOgGodkjenningsbehov(
            behandlingTags = behandlingTags,
            vedtakBegrunnelse = vedtakBegrunnelse,
            skjønnsfastsettelse = SkjønnsfastsattSykepengegrunnlagDto(
                type = SkjønnsfastsettingstypeDto.ANNET,
                årsak = SkjønnsfastsettingsårsakDto.ANDRE_AVSNITT,
                skjæringstidspunkt = skjæringstidspunkt,
                begrunnelseFraMal = skjønnsfastsettelseBegrunnelseFraMal,
                begrunnelseFraFritekst = skjønnsfastsettelseBegrunnelseFraFritekst,
                begrunnelseFraKonklusjon = skjønnsfastsettelseBegrunnelseFraKonklusjon,
                opprettet = LocalDateTime.now().minusDays(1),
            ),
            avviksvurdering = lagAvviksvurdering(
                avviksprosent = avviksprosent,
                innrapportertÅrsinntekt = innrapportertÅrsinntekt,
                omregnetÅrsinntekt = omregnetÅrsinntekt
            )
        )

        // When:
        testRapid.sendTestMessage(
            skjønnsfastsettelseMelding(
                sykepengegrunnlag = skjønnsfastsattBeløp,
                omregnetÅrsinntekt = omregnetÅrsinntekt,
                seksG = seksG,
                skjønnsfastsatt = skjønnsfastsattBeløp,
            )
        )

        // Then:
        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(1, meldinger.size)
        assertEquals(fødselsnummer, meldinger.single().key)
        val actualJsonNode = meldinger.single().json

        @Language("JSON")
        val expectedJson = """
            {
              "@event_name": "vedtak_fattet",
              "fødselsnummer": "$fødselsnummer",
              "aktørId": "$aktørId",
              "yrkesaktivitetstype" : "ARBEIDSTAKER",
              "vedtaksperiodeId": "$vedtaksperiodeId",
              "behandlingId": "$spleisBehandlingId",
              "organisasjonsnummer": "$organisasjonsnummer",
              "fom": "$fom",
              "tom": "$tom",
              "skjæringstidspunkt": "$skjæringstidspunkt",
              "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlag": $skjønnsfastsattBeløp,
              "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
              "utbetalingId": "$utbetalingId",
              "tags": [ ${behandlingTags.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlagsfakta": {
                "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                "innrapportertÅrsinntekt": $innrapportertÅrsinntekt,
                "avviksprosent": $avviksprosent,
                "6G": $seksG,
                "tags": [],
                "arbeidsgivere": [
                  {
                    "arbeidsgiver": "$organisasjonsnummer",
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
                  "begrunnelse": "$skjønnsfastsettelseBegrunnelseFraMal",
                  "perioder": [
                    {
                      "fom": "$fom",
                      "tom": "$tom"
                    }
                  ]
                },
                {
                  "type": "SkjønnsfastsattSykepengegrunnlagFritekst",
                  "begrunnelse": "$skjønnsfastsettelseBegrunnelseFraFritekst",
                  "perioder": [
                    {
                      "fom": "$fom",
                      "tom": "$tom"
                    }
                  ]
                },
                {
                  "type": "SkjønnsfastsattSykepengegrunnlagKonklusjon",
                  "begrunnelse": "$skjønnsfastsettelseBegrunnelseFraKonklusjon",
                  "perioder": [
                    {
                      "fom": "$fom",
                      "tom": "$tom"
                    }
                  ]
                },
                {
                  "type" : "Innvilgelse",
                  "begrunnelse" : "$vedtakBegrunnelse",
                  "perioder" : [
                    {
                      "fom" : "$fom",
                      "tom" : "$tom"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        assertJsonEquals(expectedJson, actualJsonNode)
    }

    @Test
    fun `fastsatt etter skjønn - delvis innvilgelse`() {
        // Given:
        val skjønnsfastsattBeløp = BigDecimal("650000.00")
        val omregnetÅrsinntekt = BigDecimal("800000.00")
        val seksG = BigDecimal("666666.66")
        val innrapportertÅrsinntekt = BigDecimal("1200000.00")
        val avviksprosent = 50.0
        val behandlingTags = listOf("Behandling tag 1", "Behandling tag 2")
        val vedtakBegrunnelse = "Begrunnelse for delvis innvilgelse"
        val skjønnsfastsettelseBegrunnelseFraMal = "Begrunnelse fra mal her"
        val skjønnsfastsettelseBegrunnelseFraFritekst = "Begrunnelse fra fritekst her"
        val skjønnsfastsettelseBegrunnelseFraKonklusjon = "Begrunnelse fra konklusjon her"
        initPersonOgGodkjenningsbehov(
            behandlingTags = behandlingTags,
            vedtakUtfall = Utfall.DELVIS_INNVILGELSE,
            vedtakBegrunnelse = vedtakBegrunnelse,
            skjønnsfastsettelse = SkjønnsfastsattSykepengegrunnlagDto(
                type = SkjønnsfastsettingstypeDto.ANNET,
                årsak = SkjønnsfastsettingsårsakDto.ANDRE_AVSNITT,
                skjæringstidspunkt = skjæringstidspunkt,
                begrunnelseFraMal = skjønnsfastsettelseBegrunnelseFraMal,
                begrunnelseFraFritekst = skjønnsfastsettelseBegrunnelseFraFritekst,
                begrunnelseFraKonklusjon = skjønnsfastsettelseBegrunnelseFraKonklusjon,
                opprettet = LocalDateTime.now().minusDays(1),
            ),
            avviksvurdering = lagAvviksvurdering(
                avviksprosent = avviksprosent,
                innrapportertÅrsinntekt = innrapportertÅrsinntekt,
                omregnetÅrsinntekt = omregnetÅrsinntekt
            )
        )

        // When:
        testRapid.sendTestMessage(
            skjønnsfastsettelseMelding(
                sykepengegrunnlag = skjønnsfastsattBeløp,
                omregnetÅrsinntekt = omregnetÅrsinntekt,
                seksG = seksG,
                skjønnsfastsatt = skjønnsfastsattBeløp,
            )
        )

        // Then:
        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(1, meldinger.size)
        assertEquals(fødselsnummer, meldinger.single().key)
        val actualJsonNode = meldinger.single().json

        @Language("JSON")
        val expectedJson = """
            {
              "@event_name": "vedtak_fattet",
              "fødselsnummer": "$fødselsnummer",
              "aktørId": "$aktørId",
              "yrkesaktivitetstype" : "ARBEIDSTAKER",
              "vedtaksperiodeId": "$vedtaksperiodeId",
              "behandlingId": "$spleisBehandlingId",
              "organisasjonsnummer": "$organisasjonsnummer",
              "fom": "$fom",
              "tom": "$tom",
              "skjæringstidspunkt": "$skjæringstidspunkt",
              "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlag": $skjønnsfastsattBeløp,
              "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
              "utbetalingId": "$utbetalingId",
              "tags": [ ${behandlingTags.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlagsfakta": {
                "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                "innrapportertÅrsinntekt": $innrapportertÅrsinntekt,
                "avviksprosent": $avviksprosent,
                "6G": $seksG,
                "tags": [],
                "arbeidsgivere": [
                  {
                    "arbeidsgiver": "$organisasjonsnummer",
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
                  "begrunnelse": "$skjønnsfastsettelseBegrunnelseFraMal",
                  "perioder": [
                    {
                      "fom": "$fom",
                      "tom": "$tom"
                    }
                  ]
                },
                {
                  "type": "SkjønnsfastsattSykepengegrunnlagFritekst",
                  "begrunnelse": "$skjønnsfastsettelseBegrunnelseFraFritekst",
                  "perioder": [
                    {
                      "fom": "$fom",
                      "tom": "$tom"
                    }
                  ]
                },
                {
                  "type": "SkjønnsfastsattSykepengegrunnlagKonklusjon",
                  "begrunnelse": "$skjønnsfastsettelseBegrunnelseFraKonklusjon",
                  "perioder": [
                    {
                      "fom": "$fom",
                      "tom": "$tom"
                    }
                  ]
                },
                {
                  "type" : "DelvisInnvilgelse",
                  "begrunnelse" : "$vedtakBegrunnelse",
                  "perioder" : [
                    {
                      "fom" : "$fom",
                      "tom" : "$tom"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        assertJsonEquals(expectedJson, actualJsonNode)
    }

    @Test
    fun `fastsatt etter hovedregel`() {
        // Given:
        val omregnetÅrsinntekt = BigDecimal("600000.00")
        val seksG = BigDecimal("666666.66")
        val innrapportertÅrsinntekt = BigDecimal("660000.00")
        val avviksprosent = 10.0
        val behandlingTags = listOf("Behandling tag 1", "Behandling tag 2")
        val vedtakBegrunnelse = "Begrunnelse for innvilgelse"
        initPersonOgGodkjenningsbehov(
            behandlingTags = behandlingTags,
            vedtakBegrunnelse = vedtakBegrunnelse,
            avviksvurdering = lagAvviksvurdering(
                avviksprosent = avviksprosent,
                innrapportertÅrsinntekt = innrapportertÅrsinntekt,
                omregnetÅrsinntekt = omregnetÅrsinntekt
            )
        )

        // When:
        testRapid.sendTestMessage(
            etterHovedregelMelding(
                sykepengegrunnlag = omregnetÅrsinntekt,
                omregnetÅrsinntekt = omregnetÅrsinntekt,
                seksG = seksG,
            )
        )

        // Then:
        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(1, meldinger.size)
        assertEquals(fødselsnummer, meldinger.single().key)
        val actualJsonNode = meldinger.single().json

        @Language("JSON")
        val expectedJson = """
            {
              "@event_name": "vedtak_fattet",
              "fødselsnummer": "$fødselsnummer",
              "aktørId": "$aktørId",
              "yrkesaktivitetstype" : "ARBEIDSTAKER",
              "vedtaksperiodeId": "$vedtaksperiodeId",
              "behandlingId": "$spleisBehandlingId",
              "organisasjonsnummer": "$organisasjonsnummer",
              "fom": "$fom",
              "tom": "$tom",
              "skjæringstidspunkt": "$skjæringstidspunkt",
              "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlag": $omregnetÅrsinntekt,
              "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
              "utbetalingId": "$utbetalingId",
              "tags": [ ${behandlingTags.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlagsfakta": {
                "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                "innrapportertÅrsinntekt": $innrapportertÅrsinntekt,
                "avviksprosent": $avviksprosent,
                "6G": $seksG,
                "tags": [],
                "arbeidsgivere": [
                  {
                    "arbeidsgiver": "$organisasjonsnummer",
                    "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                    "innrapportertÅrsinntekt": $innrapportertÅrsinntekt
                  }
                ],
                "fastsatt": "EtterHovedregel"
              },
              "begrunnelser": [
                {
                  "type" : "Innvilgelse",
                  "begrunnelse" : "$vedtakBegrunnelse",
                  "perioder" : [
                    {
                      "fom" : "$fom",
                      "tom" : "$tom"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        assertJsonEquals(expectedJson, actualJsonNode)
    }

    @Test
    fun `fastsatt etter hovedregel uten godkjenningsbehov`() {
        // Given:
        val omregnetÅrsinntekt = BigDecimal("600000.00")
        val seksG = BigDecimal("666666.66")
        val innrapportertÅrsinntekt = BigDecimal("660000.00")
        val avviksprosent = 10.0
        val behandlingTags = listOf("Behandling tag 1", "Behandling tag 2")
        val vedtakBegrunnelse = "Begrunnelse for innvilgelse"
        initPerson(
            behandlingTags = behandlingTags,
            vedtakBegrunnelse = vedtakBegrunnelse,
            avviksvurdering = lagAvviksvurdering(
                avviksprosent = avviksprosent,
                innrapportertÅrsinntekt = innrapportertÅrsinntekt,
                omregnetÅrsinntekt = omregnetÅrsinntekt
            )
        )

        // When:
        testRapid.sendTestMessage(
            etterHovedregelMelding(
                sykepengegrunnlag = omregnetÅrsinntekt,
                omregnetÅrsinntekt = omregnetÅrsinntekt,
                seksG = seksG,
            )
        )

        // Then:
        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(1, meldinger.size)
        assertEquals(fødselsnummer, meldinger.single().key)
        val actualJsonNode = meldinger.single().json

        @Language("JSON")
        val expectedJson = """
            {
              "@event_name": "vedtak_fattet",
              "fødselsnummer": "$fødselsnummer",
              "aktørId": "$aktørId",
              "yrkesaktivitetstype" : "ARBEIDSTAKER",
              "vedtaksperiodeId": "$vedtaksperiodeId",
              "behandlingId": "$spleisBehandlingId",
              "organisasjonsnummer": "$organisasjonsnummer",
              "fom": "$fom",
              "tom": "$tom",
              "skjæringstidspunkt": "$skjæringstidspunkt",
              "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlag": $omregnetÅrsinntekt,
              "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
              "utbetalingId": "$utbetalingId",
              "tags": [ ${behandlingTags.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlagsfakta": {
                "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                "innrapportertÅrsinntekt": $innrapportertÅrsinntekt,
                "avviksprosent": $avviksprosent,
                "6G": $seksG,
                "tags": [],
                "arbeidsgivere": [
                  {
                    "arbeidsgiver": "$organisasjonsnummer",
                    "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                    "innrapportertÅrsinntekt": $innrapportertÅrsinntekt
                  }
                ],
                "fastsatt": "EtterHovedregel"
              },
              "begrunnelser": [
                {
                  "type" : "Innvilgelse",
                  "begrunnelse" : "$vedtakBegrunnelse",
                  "perioder" : [
                    {
                      "fom" : "$fom",
                      "tom" : "$tom"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        assertJsonEquals(expectedJson, actualJsonNode)
    }

    @Test
    fun `fastsatt etter hovedregel - delvis innvilgelse`() {
        // Given:
        val omregnetÅrsinntekt = BigDecimal("600000.00")
        val seksG = BigDecimal("666666.66")
        val innrapportertÅrsinntekt = BigDecimal("660000.00")
        val avviksprosent = 10.0
        val behandlingTags = listOf("Behandling tag 1", "Behandling tag 2")
        val vedtakBegrunnelse = "Begrunnelse for delvis innvilgelse"
        initPersonOgGodkjenningsbehov(
            behandlingTags = behandlingTags,
            vedtakUtfall = Utfall.DELVIS_INNVILGELSE,
            vedtakBegrunnelse = vedtakBegrunnelse,
            avviksvurdering = lagAvviksvurdering(
                avviksprosent = avviksprosent,
                innrapportertÅrsinntekt = innrapportertÅrsinntekt,
                omregnetÅrsinntekt = omregnetÅrsinntekt
            )
        )

        // When:
        testRapid.sendTestMessage(
            etterHovedregelMelding(
                sykepengegrunnlag = omregnetÅrsinntekt,
                omregnetÅrsinntekt = omregnetÅrsinntekt,
                seksG = seksG,
            )
        )

        // Then:
        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(1, meldinger.size)
        assertEquals(fødselsnummer, meldinger.single().key)
        val actualJsonNode = meldinger.single().json

        @Language("JSON")
        val expectedJson = """
            {
              "@event_name": "vedtak_fattet",
              "fødselsnummer": "$fødselsnummer",
              "aktørId": "$aktørId",
              "yrkesaktivitetstype" : "ARBEIDSTAKER",
              "vedtaksperiodeId": "$vedtaksperiodeId",
              "behandlingId": "$spleisBehandlingId",
              "organisasjonsnummer": "$organisasjonsnummer",
              "fom": "$fom",
              "tom": "$tom",
              "skjæringstidspunkt": "$skjæringstidspunkt",
              "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlag": $omregnetÅrsinntekt,
              "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
              "utbetalingId": "$utbetalingId",
              "tags": [ ${behandlingTags.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlagsfakta": {
                "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                "innrapportertÅrsinntekt": $innrapportertÅrsinntekt,
                "avviksprosent": $avviksprosent,
                "6G": $seksG,
                "tags": [],
                "arbeidsgivere": [
                  {
                    "arbeidsgiver": "$organisasjonsnummer",
                    "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                    "innrapportertÅrsinntekt": $innrapportertÅrsinntekt
                  }
                ],
                "fastsatt": "EtterHovedregel"
              },
              "begrunnelser": [
                {
                  "type" : "DelvisInnvilgelse",
                  "begrunnelse" : "$vedtakBegrunnelse",
                  "perioder" : [
                    {
                      "fom" : "$fom",
                      "tom" : "$tom"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        assertJsonEquals(expectedJson, actualJsonNode)
    }

    @Test
    fun `fastsatt etter hovedregel begrenset til 6G`() {
        // Given:
        val omregnetÅrsinntekt = BigDecimal("900000.00")
        val seksG = BigDecimal("666666.66")
        val innrapportertÅrsinntekt = BigDecimal("990000.00")
        val avviksprosent = 10.0
        val behandlingTags = listOf("Behandling tag 1", "Behandling tag 2")
        val vedtakBegrunnelse = "Begrunnelse for innvilgelse"
        initPersonOgGodkjenningsbehov(
            behandlingTags = behandlingTags + "6GBegrenset",
            vedtakBegrunnelse = vedtakBegrunnelse,
            avviksvurdering = lagAvviksvurdering(
                avviksprosent = avviksprosent,
                innrapportertÅrsinntekt = innrapportertÅrsinntekt,
                omregnetÅrsinntekt = omregnetÅrsinntekt
            )
        )

        // When:
        testRapid.sendTestMessage(
            etterHovedregelMelding(
                sykepengegrunnlag = seksG,
                omregnetÅrsinntekt = omregnetÅrsinntekt,
                seksG = seksG,
            )
        )

        // Then:
        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(1, meldinger.size)
        assertEquals(fødselsnummer, meldinger.single().key)
        val actualJsonNode = meldinger.single().json

        @Language("JSON")
        val expectedJson = """
            {
              "@event_name": "vedtak_fattet",
              "fødselsnummer": "$fødselsnummer",
              "aktørId": "$aktørId",
              "yrkesaktivitetstype" : "ARBEIDSTAKER",
              "vedtaksperiodeId": "$vedtaksperiodeId",
              "behandlingId": "$spleisBehandlingId",
              "organisasjonsnummer": "$organisasjonsnummer",
              "fom": "$fom",
              "tom": "$tom",
              "skjæringstidspunkt": "$skjæringstidspunkt",
              "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlag": $seksG,
              "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
              "utbetalingId": "$utbetalingId",
              "tags": [ ${behandlingTags.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlagsfakta": {
                "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                "innrapportertÅrsinntekt": $innrapportertÅrsinntekt,
                "avviksprosent": $avviksprosent,
                "6G": $seksG,
                "tags": [ "6GBegrenset" ],
                "arbeidsgivere": [
                  {
                    "arbeidsgiver": "$organisasjonsnummer",
                    "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                    "innrapportertÅrsinntekt": $innrapportertÅrsinntekt
                  }
                ],
                "fastsatt": "EtterHovedregel"
              },
              "begrunnelser": [
                {
                  "type" : "Innvilgelse",
                  "begrunnelse" : "$vedtakBegrunnelse",
                  "perioder" : [
                    {
                      "fom" : "$fom",
                      "tom" : "$tom"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        assertJsonEquals(expectedJson, actualJsonNode)
    }

    @Test
    fun `fastsatt i infotrygd`() {
        // Given:
        val omregnetÅrsinntekt = BigDecimal("600000.00")
        val innrapportertÅrsinntekt = BigDecimal("660000.00")
        val avviksprosent = 10.0
        val behandlingTags = listOf("Behandling tag 1", "Behandling tag 2")
        val vedtakBegrunnelse = "Begrunnelse for innvilgelse"
        initPersonOgGodkjenningsbehov(
            behandlingTags = behandlingTags,
            vedtakBegrunnelse = vedtakBegrunnelse,
            avviksvurdering = lagAvviksvurdering(
                avviksprosent = avviksprosent,
                innrapportertÅrsinntekt = innrapportertÅrsinntekt,
                omregnetÅrsinntekt = omregnetÅrsinntekt
            )
        )

        // When:
        testRapid.sendTestMessage(
            fastsattIInfotrygdMelding(
                sykepengegrunnlag = omregnetÅrsinntekt,
                omregnetÅrsinntekt = omregnetÅrsinntekt,
            )
        )

        // Then:
        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(1, meldinger.size)
        assertEquals(fødselsnummer, meldinger.single().key)
        val actualJsonNode = meldinger.single().json

        @Language("JSON")
        val expectedJson = """
            {
              "@event_name": "vedtak_fattet",
              "fødselsnummer": "$fødselsnummer",
              "aktørId": "$aktørId",
              "yrkesaktivitetstype" : "ARBEIDSTAKER",
              "vedtaksperiodeId": "$vedtaksperiodeId",
              "behandlingId": "$spleisBehandlingId",
              "organisasjonsnummer": "$organisasjonsnummer",
              "fom": "$fom",
              "tom": "$tom",
              "skjæringstidspunkt": "$skjæringstidspunkt",
              "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlag": $omregnetÅrsinntekt,
              "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
              "utbetalingId": "$utbetalingId",
              "tags": [ ${behandlingTags.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlagsfakta": {
                "fastsatt": "IInfotrygd",
                "omregnetÅrsinntekt": $omregnetÅrsinntekt
              },
              "begrunnelser": [
                {
                  "type" : "Innvilgelse",
                  "begrunnelse" : "$vedtakBegrunnelse",
                  "perioder" : [
                    {
                      "fom" : "$fom",
                      "tom" : "$tom"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        assertJsonEquals(expectedJson, actualJsonNode)
    }

    private fun initGodkjenningsbehov(yrkesaktivitetstype: Yrkesaktivitetstype) {
        val godkjenningsbehovId = UUID.randomUUID()
        val godkjenningsbehovJson = lagGodkjenningsbehov(
            id = godkjenningsbehovId,
            aktørId = lagAktørId(),
            fødselsnummer = lagFødselsnummer(),
            spleisBehandlingId = spleisBehandlingId,
            yrkesaktivitetstype = yrkesaktivitetstype,
            sykepengegrunnlagsfakta = when (yrkesaktivitetstype) {
                Yrkesaktivitetstype.ARBEIDSTAKER -> godkjenningsbehovFastsattEtterHovedregel(
                    arbeidsgivere = listOf(
                        mapOf(
                            "arbeidsgiver" to organisasjonsnummer,
                            "omregnetÅrsinntekt" to 600000.00,
                            "inntektskilde" to "Arbeidsgiver"
                        )
                    )
                )

                Yrkesaktivitetstype.SELVSTENDIG -> godkjenningsbehovSelvstendigNæringsdrivende(
                    sykepengegrunnlag = BigDecimal(600000),
                    seksG = BigDecimal(666666),
                    beregningsgrunnlag = BigDecimal(600000),
                    pensjonsgivendeInntekter = (2022..2024).map { år -> år to BigDecimal(200000) }
                )

                Yrkesaktivitetstype.FRILANS,
                Yrkesaktivitetstype.ARBEIDSLEDIG -> error("Ukjent yrkesaktivitetstype: $yrkesaktivitetstype")
            }
        )
        vedtakDao.vedtaksperiodeHendelseTabell.add(vedtaksperiodeId to godkjenningsbehovId)
        meldingDao.godkjenningsbehov.add(Godkjenningsbehov.fraJson(godkjenningsbehovJson))
    }

    @Test
    fun `selvstendig næringsdrivende`() {
        // Given:
        val beregningsgrunnlag = BigDecimal("600000.00")
        val seksG = BigDecimal("666666.66")
        val behandlingTags = listOf("Behandling tag 1", "Behandling tag 2")
        val vedtakBegrunnelse = "Begrunnelse for innvilgelse"
        initPersonOgGodkjenningsbehov(
            behandlingTags = behandlingTags,
            vedtakBegrunnelse = vedtakBegrunnelse,
            organisasjonsnummer = "SELVSTENDIG",
            yrkesaktivitetstype = Yrkesaktivitetstype.SELVSTENDIG
        )

        // When:
        testRapid.sendTestMessage(
            selvstendigNæringsdrivendeMelding(
                sykepengegrunnlag = beregningsgrunnlag,
                beregningsgrunnlag = beregningsgrunnlag,
                seksG = seksG,
            )
        )

        // Then:
        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(1, meldinger.size)
        assertEquals(fødselsnummer, meldinger.single().key)
        val actualJsonNode = meldinger.single().json

        @Language("JSON")
        val expectedJson = """
            {
              "@event_name": "vedtak_fattet",
              "fødselsnummer": "$fødselsnummer",
              "aktørId": "$aktørId",
              "yrkesaktivitetstype": "SELVSTENDIG",
              "vedtaksperiodeId": "$vedtaksperiodeId",
              "behandlingId": "$spleisBehandlingId",
              "organisasjonsnummer" : "SELVSTENDIG",
              "fom": "$fom",
              "tom": "$tom",
              "skjæringstidspunkt": "$skjæringstidspunkt",
              "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlag": $beregningsgrunnlag,
              "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
              "utbetalingId": "$utbetalingId",
              "tags": [ ${behandlingTags.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlagsfakta": {
                "fastsatt": "EtterHovedregel",
                "6G": $seksG,
                "tags" : [ ],
                "selvstendig": {
                  "beregningsgrunnlag": $beregningsgrunnlag,
                  "pensjonsgivendeInntekter" : [ 
                      {
                        "årstall" : 2022,
                        "beløp" : 200000
                      }, {
                        "årstall" : 2023,
                        "beløp" : 200000
                      }, {
                        "årstall" : 2024,
                        "beløp" : 200000
                      } 
                  ]
                }
              },
              "begrunnelser": [
                {
                  "type" : "Innvilgelse",
                  "begrunnelse" : "$vedtakBegrunnelse",
                  "perioder" : [
                    {
                      "fom" : "$fom",
                      "tom" : "$tom"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        assertJsonEquals(expectedJson, actualJsonNode)
    }

    @Test
    fun `selvstendig næringsdrivende begrenset til 6G`() {
        // Given:
        val beregningsgrunnlag = BigDecimal("800000.00")
        val seksG = BigDecimal("666666.66")
        val behandlingTags = listOf("Behandling tag 1", "Behandling tag 2")
        val vedtakBegrunnelse = "Begrunnelse for innvilgelse"
        initPersonOgGodkjenningsbehov(
            behandlingTags = behandlingTags + "6GBegrenset",
            vedtakBegrunnelse = vedtakBegrunnelse,
            organisasjonsnummer = "SELVSTENDIG",
            yrkesaktivitetstype = Yrkesaktivitetstype.SELVSTENDIG
        )

        // When:
        testRapid.sendTestMessage(
            selvstendigNæringsdrivendeMelding(
                sykepengegrunnlag = seksG,
                beregningsgrunnlag = beregningsgrunnlag,
                seksG = seksG,
            )
        )

        // Then:
        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(1, meldinger.size)
        assertEquals(fødselsnummer, meldinger.single().key)
        val actualJsonNode = meldinger.single().json

        @Language("JSON")
        val expectedJson = """
            {
              "@event_name": "vedtak_fattet",
              "fødselsnummer": "$fødselsnummer",
              "aktørId": "$aktørId",
              "yrkesaktivitetstype": "SELVSTENDIG",
              "vedtaksperiodeId": "$vedtaksperiodeId",
              "behandlingId": "$spleisBehandlingId",
              "organisasjonsnummer" : "SELVSTENDIG",
              "fom": "$fom",
              "tom": "$tom",
              "skjæringstidspunkt": "$skjæringstidspunkt",
              "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlag": $seksG,
              "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
              "utbetalingId": "$utbetalingId",
              "tags": [ ${behandlingTags.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlagsfakta": {
                "fastsatt": "EtterHovedregel",
                "6G": $seksG,
                "tags" : [ "6GBegrenset" ],
                "selvstendig": {
                  "beregningsgrunnlag": $beregningsgrunnlag,
                  "pensjonsgivendeInntekter" : [ 
                      {
                        "årstall" : 2022,
                        "beløp" : 200000
                      }, {
                        "årstall" : 2023,
                        "beløp" : 200000
                      }, {
                        "årstall" : 2024,
                        "beløp" : 200000
                      } 
                  ]
                }
              },
              "begrunnelser": [
                {
                  "type" : "Innvilgelse",
                  "begrunnelse" : "$vedtakBegrunnelse",
                  "perioder" : [
                    {
                      "fom" : "$fom",
                      "tom" : "$tom"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        assertJsonEquals(expectedJson, actualJsonNode)
    }

    private fun initPersonOgGodkjenningsbehov(
        behandlingTags: List<String>,
        vedtakUtfall: Utfall = Utfall.INNVILGELSE,
        vedtakBegrunnelse: String,
        skjønnsfastsettelse: SkjønnsfastsattSykepengegrunnlagDto? = null,
        avviksvurdering: Avviksvurdering? = null,
        organisasjonsnummer: String = this.organisasjonsnummer,
        yrkesaktivitetstype: Yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER
    ) {
        initPerson(
            organisasjonsnummer = organisasjonsnummer,
            behandlingTags = behandlingTags,
            vedtakUtfall = vedtakUtfall,
            vedtakBegrunnelse = vedtakBegrunnelse,
            yrkesaktivitetstype = yrkesaktivitetstype,
            skjønnsfastsettelse = skjønnsfastsettelse,
            avviksvurdering = avviksvurdering
        )
        initGodkjenningsbehov(yrkesaktivitetstype)
    }

    private fun initPerson(
        behandlingTags: List<String>,
        vedtakUtfall: Utfall = Utfall.INNVILGELSE,
        vedtakBegrunnelse: String,
        skjønnsfastsettelse: SkjønnsfastsattSykepengegrunnlagDto? = null,
        avviksvurdering: Avviksvurdering? = null,
        organisasjonsnummer: String = this.organisasjonsnummer,
        yrkesaktivitetstype: Yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER
    ) {
        legacyPersonRepository.leggTilPerson(
            Person.gjenopprett(
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                vedtaksperioder = listOf(
                    VedtaksperiodeDto(
                        organisasjonsnummer = organisasjonsnummer,
                        vedtaksperiodeId = vedtaksperiodeId,
                        forkastet = false,
                        behandlinger = listOf(
                            BehandlingDto(
                                id = behandlingId,
                                vedtaksperiodeId = vedtaksperiodeId,
                                utbetalingId = utbetalingId,
                                spleisBehandlingId = spleisBehandlingId,
                                skjæringstidspunkt = skjæringstidspunkt,
                                fom = fom,
                                tom = tom,
                                tilstand = TilstandDto.KlarTilBehandling,
                                tags = behandlingTags,
                                vedtakBegrunnelse = VedtakBegrunnelse(
                                    utfall = vedtakUtfall,
                                    begrunnelse = vedtakBegrunnelse
                                ),
                                varsler = emptyList(),
                                yrkesaktivitetstype = yrkesaktivitetstype
                            )
                        )
                    )
                ),
                skjønnsfastsattSykepengegrunnlag = listOfNotNull(skjønnsfastsettelse),
                avviksvurderinger = listOfNotNull(avviksvurdering)
            )
        )
    }

    private fun lagAvviksvurdering(
        organisasjonsnummer: String = this.organisasjonsnummer,
        avviksprosent: Double,
        innrapportertÅrsinntekt: BigDecimal,
        omregnetÅrsinntekt: BigDecimal
    ): Avviksvurdering = Avviksvurdering(
        unikId = UUID.randomUUID(),
        vilkårsgrunnlagId = UUID.randomUUID(),
        fødselsnummer = fødselsnummer,
        skjæringstidspunkt = skjæringstidspunkt,
        opprettet = LocalDateTime.now(),
        avviksprosent = avviksprosent,
        sammenligningsgrunnlag = Sammenligningsgrunnlag(
            totalbeløp = innrapportertÅrsinntekt.toDouble(),
            innrapporterteInntekter = listOf(
                InnrapportertInntekt(
                    arbeidsgiverreferanse = organisasjonsnummer,
                    inntekter = (1..12).map {
                        Inntekt(
                            årMåned = YearMonth.of(
                                skjæringstidspunkt.year - 1,
                                Month.of(it)
                            ),
                            beløp = (innrapportertÅrsinntekt / BigDecimal(12)).toDouble()
                        )
                    }
                )
            )
        ),
        beregningsgrunnlag = Beregningsgrunnlag(
            totalbeløp = omregnetÅrsinntekt.toDouble(),
            omregnedeÅrsinntekter = listOf(
                OmregnetÅrsinntekt(
                    arbeidsgiverreferanse = organisasjonsnummer,
                    beløp = omregnetÅrsinntekt.toDouble()
                )
            )
        )
    )

    private fun assertJsonEquals(expectedJson: String, actualJsonNode: JsonNode) {
        val writer = objectMapper.writerWithDefaultPrettyPrinter()
        assertEquals(
            writer.writeValueAsString(objectMapper.readTree(expectedJson)),
            writer.writeValueAsString(actualJsonNode)
        )
    }

    @Language("JSON")
    private fun skjønnsfastsettelseMelding(
        sykepengegrunnlag: BigDecimal,
        omregnetÅrsinntekt: BigDecimal,
        seksG: BigDecimal,
        skjønnsfastsatt: BigDecimal
    ) = """
        {
          "@event_name": "avsluttet_med_vedtak",
          "organisasjonsnummer": "$organisasjonsnummer",
          "yrkesaktivitetstype": "ARBEIDSTAKER",
          "vedtaksperiodeId": "$vedtaksperiodeId",
          "behandlingId": "$spleisBehandlingId",
          "fom": "$fom",
          "tom": "$tom",
          "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
          "skjæringstidspunkt": "$skjæringstidspunkt",
          "sykepengegrunnlag": $sykepengegrunnlag,
          "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
          "utbetalingId": "$utbetalingId",
          "sykepengegrunnlagsfakta": {
            "fastsatt": "EtterSkjønn",
            "omregnetÅrsinntekt": $omregnetÅrsinntekt,
            "omregnetÅrsinntektTotalt": $omregnetÅrsinntekt,
            "6G": $seksG,
            "arbeidsgivere": [
              {
                "arbeidsgiver": "$organisasjonsnummer",
                "omregnetÅrsinntekt": $omregnetÅrsinntekt,
                "skjønnsfastsatt": $skjønnsfastsatt,
                "inntektskilde": "Saksbehandler"
              }
            ],
            "skjønnsfastsatt": $skjønnsfastsatt
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
          "fødselsnummer": "$fødselsnummer",
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
    private fun etterHovedregelMelding(
        sykepengegrunnlag: BigDecimal,
        omregnetÅrsinntekt: BigDecimal,
        seksG: BigDecimal,
    ) = """
        {
          "@event_name": "avsluttet_med_vedtak",
          "organisasjonsnummer": "$organisasjonsnummer",
          "yrkesaktivitetstype": "ARBEIDSTAKER",
          "vedtaksperiodeId": "$vedtaksperiodeId",
          "behandlingId": "$spleisBehandlingId",
          "fom": "$fom",
          "tom": "$tom",
          "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
          "skjæringstidspunkt": "$skjæringstidspunkt",
          "sykepengegrunnlag": $sykepengegrunnlag,
          "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
          "utbetalingId": "$utbetalingId",
          "sykepengegrunnlagsfakta": {
            "fastsatt": "EtterHovedregel",
            "omregnetÅrsinntekt": $omregnetÅrsinntekt,
            "omregnetÅrsinntektTotalt": $omregnetÅrsinntekt,
            "sykepengegrunnlag": $sykepengegrunnlag,
            "6G": $seksG,
            "arbeidsgivere": [
              {
                "arbeidsgiver": "$organisasjonsnummer",
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
          "fødselsnummer": "$fødselsnummer",
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
    private fun fastsattIInfotrygdMelding(
        sykepengegrunnlag: BigDecimal,
        omregnetÅrsinntekt: BigDecimal,
    ) = """
        {
          "@event_name": "avsluttet_med_vedtak",
          "organisasjonsnummer": "$organisasjonsnummer",
          "yrkesaktivitetstype": "ARBEIDSTAKER",
          "vedtaksperiodeId": "$vedtaksperiodeId",
          "behandlingId": "$spleisBehandlingId",
          "fom": "$fom",
          "tom": "$tom",
          "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
          "skjæringstidspunkt": "$skjæringstidspunkt",
          "sykepengegrunnlag": $sykepengegrunnlag,
          "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
          "utbetalingId": "$utbetalingId",
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
          "fødselsnummer": "$fødselsnummer",
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
    private fun selvstendigNæringsdrivendeMelding(
        sykepengegrunnlag: BigDecimal,
        beregningsgrunnlag: BigDecimal,
        seksG: BigDecimal,
    ) = """
        {
          "@event_name": "avsluttet_med_vedtak",
          "organisasjonsnummer": "SELVSTENDIG",
          "yrkesaktivitetstype": "SELVSTENDIG",
          "vedtaksperiodeId": "$vedtaksperiodeId",
          "behandlingId": "$spleisBehandlingId",
          "fom": "$fom",
          "tom": "$tom",
          "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
          "skjæringstidspunkt": "$skjæringstidspunkt",
          "sykepengegrunnlag": $sykepengegrunnlag,
          "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
          "utbetalingId": "$utbetalingId",
          "sykepengegrunnlagsfakta": {
            "fastsatt": "EtterHovedregel",
            "omregnetÅrsinntekt": 0.0,
            "omregnetÅrsinntektTotalt": 0.0,
            "sykepengegrunnlag": $sykepengegrunnlag,
            "6G": $seksG,
            "arbeidsgivere": [
              {
                "arbeidsgiver": "SELVSTENDIG",
                "omregnetÅrsinntekt": $beregningsgrunnlag,
                "inntektskilde": "AOrdningen"
              }
            ]
          },
          "@id": "60839d76-0619-4028-8ae0-9946088335f3",
          "@opprettet": "2025-08-07T16:01:33.811264213",
          "system_read_count": 1,
          "system_participating_services": [
            {
              "id": "60839d76-0619-4028-8ae0-9946088335f3",
              "time": "2025-08-07T16:01:33.811264213",
              "service": "helse-spleis",
              "instance": "helse-spleis-655b9d9d9-4zqfh",
              "image": "europe-north1-docker.pkg.dev/nais-management-233d/tbd/helse-spleis-spleis:2025.08.07-09.50-53e68d5"
            },
            {
              "id": "60839d76-0619-4028-8ae0-9946088335f3",
              "time": "2025-08-07T16:01:34.014712156",
              "service": "spesialist",
              "instance": "spesialist-67995bc5d-gcwgt",
              "image": "europe-north1-docker.pkg.dev/nais-management-233d/tbd/helse-spesialist:2025.08.07-13.55-163f597"
            }
          ],
          "fødselsnummer": "$fødselsnummer",
          "@forårsaket_av": {
            "id": "d4a5fe16-3261-4a3f-a82e-f0c303a73c29",
            "opprettet": "2025-08-07T16:01:33.744250322",
            "event_name": "behov",
            "behov": [
              "Utbetaling"
            ]
          }
        }
    """.trimIndent()
}
