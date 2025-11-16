package no.nav.helse.spesialist.kafka.rivers

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.modell.vedtak.Utfall
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.VedtakBegrunnelse
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagVedtakBegrunnelse
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import no.nav.helse.spesialist.kafka.IntegrationTestFixture
import no.nav.helse.spesialist.kafka.TestRapidHelpers.publiserteMeldingerUtenGenererteFelter
import no.nav.helse.spesialist.kafka.objectMapper
import no.nav.helse.spesialist.kafka.testfixtures.Testmeldingfabrikk.godkjenningsbehovSelvstendigNæringsdrivende
import no.nav.helse.spesialist.kafka.testfixtures.Testmeldingfabrikk.lagGodkjenningsbehov
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class AvsluttetMedVedtakRiverSelvstendigNæringsdrivendeIntegrationTest {
    private val testRapid = TestRapid()
    private val integrationTestFixture = IntegrationTestFixture(testRapid)
    private val sessionContext = integrationTestFixture.sessionFactory.sessionContext

    private val hendelser = listOf(UUID.randomUUID(), UUID.randomUUID())
    private val vedtakFattetTidspunkt = LocalDateTime.now()
    private val seksG = BigDecimal("666666.66")

    private lateinit var beregningsgrunnlag: BigDecimal
    private lateinit var behandlingTags: Set<String>

    @Test
    fun `fastsatt etter hovedregel`() {
        // Given:
        this.beregningsgrunnlag = BigDecimal("600000.00")
        this.behandlingTags = setOf("Behandling tag 1", "Behandling tag 2")

        setup()

        initGodkjenningsbehov()

        // When:
        testRapid.sendTestMessage(fastsattEtterHovedregelMelding(sykepengegrunnlag = beregningsgrunnlag))

        // Then:
        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(1, meldinger.size)
        assertEquals(person.id.value, meldinger.single().key)
        val actualJsonNode = meldinger.single().json

        @Language("JSON")
        val expectedJson = """
            {
              "@event_name": "vedtak_fattet",
              "fødselsnummer": "${person.id.value}",
              "aktørId": "${person.aktørId}",
              "yrkesaktivitetstype": "SELVSTENDIG",
              "vedtaksperiodeId": "${vedtaksperiode.id.value}",
              "behandlingId": "${behandling.spleisBehandlingId?.value}",
              "organisasjonsnummer" : "SELVSTENDIG",
              "fom": "${behandling.fom}",
              "tom": "${behandling.tom}",
              "skjæringstidspunkt": "${behandling.skjæringstidspunkt}",
              "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlag": $beregningsgrunnlag,
              "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
              "utbetalingId": "${behandling.utbetalingId?.value}",
              "tags": [ ${behandling.tags.joinToString(separator = ", ") { "\"$it\"" }} ],
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
                  "begrunnelse" : "${vedtakBegrunnelse.tekst}",
                  "perioder" : [
                    {
                      "fom" : "${behandling.fom}",
                      "tom" : "${behandling.tom}"
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
        this.beregningsgrunnlag = BigDecimal("800000.00")
        this.behandlingTags = setOf("Behandling tag 1", "Behandling tag 2", "6GBegrenset")

        setup()

        initGodkjenningsbehov()

        // When:
        testRapid.sendTestMessage(fastsattEtterHovedregelMelding(sykepengegrunnlag = seksG))

        // Then:
        val meldinger = testRapid.publiserteMeldingerUtenGenererteFelter()
        assertEquals(1, meldinger.size)
        assertEquals(person.id.value, meldinger.single().key)
        val actualJsonNode = meldinger.single().json

        @Language("JSON")
        val expectedJson = """
            {
              "@event_name": "vedtak_fattet",
              "fødselsnummer": "${person.id.value}",
              "aktørId": "${person.aktørId}",
              "yrkesaktivitetstype": "SELVSTENDIG",
              "vedtaksperiodeId": "${vedtaksperiode.id.value}",
              "behandlingId": "${behandling.spleisBehandlingId?.value}",
              "organisasjonsnummer" : "SELVSTENDIG",
              "fom": "${behandling.fom}",
              "tom": "${behandling.tom}",
              "skjæringstidspunkt": "${behandling.skjæringstidspunkt}",
              "hendelser": [ ${hendelser.joinToString(separator = ", ") { "\"$it\"" }} ],
              "sykepengegrunnlag": $seksG,
              "vedtakFattetTidspunkt": "$vedtakFattetTidspunkt",
              "utbetalingId": "${behandling.utbetalingId?.value}",
              "tags": [ ${behandling.tags.minus("6GBegrenset").joinToString(separator = ", ") { "\"$it\"" }} ],
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
                  "begrunnelse" : "${vedtakBegrunnelse.tekst}",
                  "perioder" : [
                    {
                      "fom" : "${behandling.fom}",
                      "tom" : "${behandling.tom}"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        assertJsonEquals(expectedJson, actualJsonNode)
    }

    private lateinit var saksbehandler: Saksbehandler
    private lateinit var person: Person
    private lateinit var vedtaksperiode: Vedtaksperiode
    private lateinit var behandling: Behandling
    private lateinit var vedtakBegrunnelse: VedtakBegrunnelse

    private fun setup() {
        this.saksbehandler = lagSaksbehandler()
            .also(sessionContext.saksbehandlerRepository::lagre)

        this.person = lagPerson()
            .also(sessionContext.personRepository::lagre)

        this.vedtaksperiode = lagVedtaksperiode(
            identitetsnummer = person.id,
            organisasjonsnummer = "SELVSTENDIG"
        ).also(sessionContext.vedtaksperiodeRepository::lagre)

        this.behandling = lagBehandling(
            vedtaksperiodeId = vedtaksperiode.id,
            tags = behandlingTags,
            yrkesaktivitetstype = Yrkesaktivitetstype.SELVSTENDIG
        ).also(sessionContext.behandlingRepository::lagre)

        this.vedtakBegrunnelse = lagVedtakBegrunnelse(
            spleisBehandlingId = behandling.spleisBehandlingId!!,
            utfall = Utfall.INNVILGELSE,
            saksbehandlerOid = saksbehandler.id
        ).also(sessionContext.vedtakBegrunnelseRepository::lagre)
    }

    private fun initGodkjenningsbehov() {
        val godkjenningsbehovId = UUID.randomUUID()
        val godkjenningsbehovJson = lagGodkjenningsbehov(
            id = godkjenningsbehovId,
            aktørId = person.aktørId,
            fødselsnummer = person.id.value,
            spleisBehandlingId = behandling.spleisBehandlingId!!.value,
            yrkesaktivitetstype = Yrkesaktivitetstype.SELVSTENDIG,
            sykepengegrunnlagsfakta = godkjenningsbehovSelvstendigNæringsdrivende(
                sykepengegrunnlag = BigDecimal(600000),
                seksG = BigDecimal(666666),
                beregningsgrunnlag = BigDecimal(600000),
                pensjonsgivendeInntekter = (2022..2024).map { år -> år to BigDecimal(200000) }
            )
        )
        sessionContext.meldingDao.godkjenningsbehov.add(Godkjenningsbehov.fraJson(godkjenningsbehovJson))
    }

    private fun assertJsonEquals(expectedJson: String, actualJsonNode: JsonNode) {
        val writer = objectMapper.writerWithDefaultPrettyPrinter()
        assertEquals(
            writer.writeValueAsString(objectMapper.readTree(expectedJson)),
            writer.writeValueAsString(actualJsonNode)
        )
    }

    @Language("JSON")
    private fun fastsattEtterHovedregelMelding(sykepengegrunnlag: BigDecimal) = """
        {
          "@event_name": "avsluttet_med_vedtak",
          "organisasjonsnummer": "SELVSTENDIG",
          "yrkesaktivitetstype": "SELVSTENDIG",
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
          "fødselsnummer": "${person.id.value}",
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
