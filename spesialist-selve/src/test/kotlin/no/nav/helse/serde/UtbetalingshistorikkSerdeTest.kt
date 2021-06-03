package no.nav.helse.serde

import AbstractE2ETest
import com.fasterxml.jackson.databind.JsonNode
import io.mockk.every
import no.nav.helse.desember
import no.nav.helse.mediator.FeatureToggle.REVURDERING_TOGGLE
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class UtbetalingshistorikkSerdeTest : AbstractE2ETest() {

    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val FØDSELSNUMMER = "12020052345"
        private const val AKTØR = "999999999"
        private const val ORGNR = "222222222"
        private const val ORGNR2 = "13370420"
        private val ID = UUID.randomUUID()
    }

    @BeforeEach
    fun setup() {
        REVURDERING_TOGGLE.enable()
        every { restClient.hentSpeilSpapshot(any()) } returns snapshotMedHistorikk()
    }

    @AfterEach
    fun tearDown() {
        REVURDERING_TOGGLE.disable()
    }

    @Test
    fun `mapper utbetalingshistorikk fra Spleis til utbetalingshistorikk til Speil`() {
        val beregningId = UUID.randomUUID()
        every { restClient.hentSpeilSpapshot(any()) } returns snapshotMedHistorikk(utbetalingshistorikk(beregningId, medVurdering = true))

        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsforholdløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        val speilSnapshot = requireNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER))

        assertEquals(1, speilSnapshot.arbeidsgivere.last().utbetalingshistorikk.size)
        val historikkElement = speilSnapshot.arbeidsgivere.last().utbetalingshistorikk.first()
        val utbetaling = historikkElement.utbetaling
        assertEquals(beregningId, historikkElement.beregningId)
        assertNotNull(historikkElement.tidsstempel)
        assertEquals(2, historikkElement.beregnettidslinje.size)
        assertEquals(1, historikkElement.hendelsetidslinje.size)
        assertEquals(1, utbetaling.utbetalingstidslinje.size)
        assertEquals(beregningId, utbetaling.beregningId)
        assertEquals(237, utbetaling.gjenståendeSykedager)
        assertEquals(11, utbetaling.forbrukteSykedager)
        assertEquals(15741, utbetaling.arbeidsgiverNettoBeløp)
        assertEquals(28.desember(2018), utbetaling.maksdato)
        assertEquals("UTBETALT", utbetaling.status)
        assertEquals("UTBETALING", utbetaling.type)
        assertEquals("EN_FAGSYSTEMID", utbetaling.arbeidsgiverFagsystemId)
        assertEquals(false, utbetaling.vurdering?.automatisk)
        assertEquals(true, utbetaling.vurdering?.godkjent)
        assertEquals("EN_IDENT", utbetaling.vurdering?.ident)
        assertNotNull(utbetaling.vurdering?.tidsstempel)
    }

    @Test
    fun `mapper utbetalingshistorikk fra Spleis til utbetalingshistorikk til Speil uten vurdering`() {
        val beregningId = UUID.randomUUID()
        every { restClient.hentSpeilSpapshot(any()) } returns snapshotMedHistorikk(utbetalingshistorikk(beregningId, medVurdering = false))

        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsforholdløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        val speilSnapshot = requireNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER))

        val utbetaling = speilSnapshot.arbeidsgivere.last().utbetalingshistorikk.first().utbetaling
        assertNull(utbetaling.vurdering)
    }

    @Test
    fun `manglende utbetalingshistorikk mapper til tom utbetalingshistorikk til Speil`() {
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsforholdløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        val speilSnapshot = requireNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER))
        assertEquals(emptyList(), speilSnapshot.arbeidsgivere.first().utbetalingshistorikk)
    }

    @Test
    fun `utbetalingshistorikk satt lik null mapper til tom utbetalingshistorikk til Speil`() {
        every { restClient.hentSpeilSpapshot(any()) } returns snapshotMedHistorikk(null)
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsforholdløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        val speilSnapshot = requireNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER))
        assertEquals(emptyList(), speilSnapshot.arbeidsgivere.last().utbetalingshistorikk)
    }

    @Test
    fun `tom utbetalingshistorikk mapper til tom utbetalingshistorikk til Speil`() {
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsforholdløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        val speilSnapshot = requireNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER))
        assertEquals(emptyList(), speilSnapshot.arbeidsgivere.last().utbetalingshistorikk)
    }


    @Test
    fun `mapper ikke ufullstending utbetalingshistorikk`() {
        val beregningId = UUID.randomUUID()
        every { restClient.hentSpeilSpapshot(any()) } returns snapshotMedHistorikk(ufullstendigUtbetalingshistorikk(beregningId))

        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID, UTBETALING_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsforholdløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        val speilSnapshot = requireNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER))

        assertEquals(0, speilSnapshot.arbeidsgivere.last().utbetalingshistorikk.size)
    }

    @Language("JSON")
    private fun utbetalingshistorikk(beregningId: UUID, medVurdering: Boolean = false) = objectMapper.readTree(
        """
                [
                    {
                        "beregningId": "$beregningId",
                        "beregnettidslinje": [
                            {
                                "dagen": "2018-01-01",
                                "type": "Sykedag",
                                "kilde": {
                                    "type": "SØKNAD",
                                    "kildeId": "${UUID.randomUUID()}"
                                },
                                "grad": 100.0
                            },
                            {
                                "dagen": "2018-01-02",
                                "type": "Sykedag",
                                "kilde": {
                                    "type": "SØKNAD",
                                    "kildeId": "${UUID.randomUUID()}"
                                },
                                "grad": 100.0
                            }
                        ],
                        "hendelsetidslinje": [
                            {
                                "dagen": "2018-01-01",
                                "type": "Sykedag",
                                "kilde": {
                                    "type": "SØKNAD",
                                    "kildeId": "${UUID.randomUUID()}"
                                },
                                "grad": 100.0
                            }
                        ],
                        "tidsstempel": "${LocalDateTime.now()}",
                        "utbetaling": {
                            "beregningId": "$beregningId",
                            "type": "UTBETALING",
                            "maksdato": "2018-12-28",
                            "status": "UTBETALT",
                            "gjenståendeSykedager": 237,
                            "forbrukteSykedager": 11,
                            "arbeidsgiverNettoBeløp": 15741,
                            "arbeidsgiverFagsystemId": "EN_FAGSYSTEMID",
                            "utbetalingstidslinje": [
                                {
                                    "type": "NavDag",
                                    "inntekt": 1431,
                                    "dato": "2018-01-01",
                                    "utbetaling": 1431,
                                    "grad": 100.0,
                                    "totalGrad": 100.0
                                }
                            ],
                            "vurdering": ${
                                if (medVurdering) """{
                                    "godkjent": true,
                                    "automatisk": false,
                                    "ident": "EN_IDENT",
                                    "tidsstempel": "${LocalDateTime.now()}"
                                }""" else null
                            }
                        }
                    }
                ]
            """
    )

    @Language("JSON")
    private fun ufullstendigUtbetalingshistorikk(beregningId: UUID) = objectMapper.readTree(
        """
                [
                  {
                    "hendelsetidslinje": [
                      {
                        "dagen": "2018-01-01",
                        "type": "Sykedag",
                        "kilde": {
                          "type": "SØKNAD",
                          "kildeId": "${UUID.randomUUID()}"
                        },
                        "grad": 100.0
                      }
                    ],
                    "utbetalinger": [
                      {
                        "beregningId": "$beregningId",
                        "type": "UTBETALING",
                        "maksdato": "2018-12-28",
                        "utbetalingstidslinje": [
                          {
                              "type": "NavDag",
                              "inntekt": 1431,
                              "dato": "2018-01-01",
                              "utbetaling": 1431,
                              "grad": 100.0,
                              "totalGrad": 100.0
                          }
                        ]
                      }
                    ]
                  }
                ]
            """.trimIndent()
    )

    @Language("JSON")
    private fun snapshotMedHistorikk(utbetalingshistorikk: JsonNode? = objectMapper.createArrayNode()) = """
        {
          "versjon": 1,
          "aktørId": "$AKTØR",
          "fødselsnummer": "$FØDSELSNUMMER",
          "arbeidsgivere": [
            {
              "id": "$ID",
              "organisasjonsnummer": "$ORGNR",
              "vedtaksperioder": [
                {
                  "id": "$VEDTAKSPERIODE_ID"
                }
              ]
            },
            {
              "id": "${UUID.randomUUID()}",
              "organisasjonsnummer": "$ORGNR2",
              "vedtaksperioder": [
                {
                  "id": "${UUID.randomUUID()}"
                }
              ],
              "utbetalingshistorikk": $utbetalingshistorikk
            }
          ],
          "inntektsgrunnlag": [
            {
              "skjæringstidspunkt": "2018-01-01",
              "sykepengegrunnlag": 581298.0
            }
          ]
        }
        """
}
