package no.nav.helse.serde

import AbstractE2ETest
import com.fasterxml.jackson.databind.JsonNode
import io.mockk.every
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

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
        every { restClient.hentSpeilSpapshot(any()) } returns snapshot()
    }

    @Test
    fun `manglende utbetalingshistorikk mapper til tom utbetalingshistorikk til Speil`() {
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        val speilSnapshot = requireNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER))
        assertEquals(emptyList(), speilSnapshot.arbeidsgivere.first().utbetalingshistorikk)
    }

    @Test
    fun `utbetalingshistorikk satt lik null mapper til tom utbetalingshistorikk til Speil`() {
        every { restClient.hentSpeilSpapshot(any()) } returns snapshot(null)
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        val speilSnapshot = requireNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER))
        assertEquals(emptyList(), speilSnapshot.arbeidsgivere.last().utbetalingshistorikk)
    }

    @Test
    fun `tom utbetalingshistorikk mapper til tom utbetalingshistorikk til Speil`() {
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        val speilSnapshot = requireNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER))
        assertEquals(emptyList(), speilSnapshot.arbeidsgivere.last().utbetalingshistorikk)
    }

    @Test
    fun `mapper utbetalingshistorikk fra Spleis til utbetalingshistorikk til Speil`() {
        val beregningId = UUID.randomUUID()
        every { restClient.hentSpeilSpapshot(any()) } returns snapshot(utbetalingshistorikk(beregningId))

        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        val speilSnapshot = requireNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER))

        assertEquals(1, speilSnapshot.arbeidsgivere.last().utbetalingshistorikk.size)
        assertEquals(beregningId, speilSnapshot.arbeidsgivere.last().utbetalingshistorikk.first().beregningId)
        assertEquals(2, speilSnapshot.arbeidsgivere.last().utbetalingshistorikk.first().beregnettidslinje.size)
        assertEquals(1, speilSnapshot.arbeidsgivere.last().utbetalingshistorikk.first().hendelsetidslinje.size)
        assertEquals(1, speilSnapshot.arbeidsgivere.last().utbetalingshistorikk.first().utbetalinger.size)
        assertEquals(1, speilSnapshot.arbeidsgivere.last().utbetalingshistorikk.first().utbetalinger.first().utbetalingstidslinje.size)
    }

    @Test
    fun `mapper ikke ufullstending utbetalingshistorikk`() {
        val beregningId = UUID.randomUUID()
        every { restClient.hentSpeilSpapshot(any()) } returns snapshot(ufullstendigUtbetalingshistorikk(beregningId))

        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendArbeidsgiverinformasjonløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        val speilSnapshot = requireNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER))

        assertEquals(0, speilSnapshot.arbeidsgivere.last().utbetalingshistorikk.size)
    }

    @Language("JSON")
    private fun utbetalingshistorikk(beregningId: UUID) = objectMapper.readTree(
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
    private fun snapshot(utbetalingshistorikk: JsonNode? = objectMapper.createArrayNode()) = """
        {
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
        """.trimIndent()
}
