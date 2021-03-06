package no.nav.helse.serde

import AbstractE2ETest
import com.fasterxml.jackson.databind.JsonNode
import io.mockk.every
import no.nav.helse.desember
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
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
        every { restClient.hentSpeilSpapshot(any()) } returns snapshotMedHistorikk()
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
        assertNotNull(historikkElement.utbetaling.tidsstempel)
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
        historikkElement.utbetaling.utbetalingstidslinje.first().let {
            assertEquals(LocalDate.of(2018, 1, 1), it.dato)
            assertEquals(100.0, it.grad)
            assertEquals("NavDag", it.type)
            assertEquals(100.0, it.totalGrad)
            assertEquals(1431, it.utbetaling)
            assertEquals(1431, it.inntekt)
            assertEquals("EN_BEGRUNNELSE", it.begrunnelser?.first())
        }
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
                            "tidsstempel": "${LocalDateTime.now()}",
                            "utbetalingstidslinje": [
                                {
                                    "type": "NavDag",
                                    "inntekt": 1431,
                                    "dato": "2018-01-01",
                                    "utbetaling": 1431,
                                    "grad": 100.0,
                                    "totalGrad": 100.0,
                                    "begrunnelser": [
                                        "EN_BEGRUNNELSE"
                                    ]
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
              ],
              "utbetalingshistorikk": $utbetalingshistorikk
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
