package no.nav.helse.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.helse.kafka.AvvikVurdertRiver
import no.nav.helse.medRivers
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.meldinger.hendelser.AvvikVurdertMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AvvikVurdertRiverTest {
    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val testRapid = TestRapid().medRivers(AvvikVurdertRiver(mediator))

    @BeforeEach
    fun beforeEach() {
        testRapid.reset()
    }

    @Test
    fun `leser avviksvurdering fra kafka`() {
        val melding = slot<AvvikVurdertMessage>()
        testRapid.sendTestMessage(event())
        verify(exactly = 1) { mediator.mottaMelding(capture(melding), any()) }
        assertEquals("653cdd83-9940-4f89-80be-3bb07bf10089".toUUID(), melding.captured.vedtaksperiodeId())
    }

    @Test
    fun `leser ikke inn avviksvurdering uten sammenligningsgrunnlag`() {
        testRapid.sendTestMessage(eventUtenSammenligningsgrunnlag())
        verify(exactly = 0) { mediator.mottaMelding(any(), any()) }
    }

    @Language("JSON")
    private fun event() =
        """
    {
      "@event_name": "avvik_vurdert",
      "fødselsnummer": "12345678910",
      "aktørId": "11111100000",
      "skjæringstidspunkt": "2018-01-01",
      "vedtaksperiodeId": "653cdd83-9940-4f89-80be-3bb07bf10089",
      "avviksvurdering": {
        "id": "cc3af2c3-fa9e-4d84-a57e-a7972226cdae",
        "opprettet": "2018-01-01T00:00:00.000",
        "vilkårsgrunnlagId": "bc3af2c3-fa9e-4d84-a57e-a7973336cdaf",
        "beregningsgrunnlag": {
          "totalbeløp": 550000.0,
          "omregnedeÅrsinntekter": [
            {
              "arbeidsgiverreferanse": "000000000",
              "beløp": 250000.0
            },
            {
              "arbeidsgiverreferanse": "000000000",
              "beløp": 300000.0
            }
          ]
        },
        "sammenligningsgrunnlag": {
          "id": "887b2e4c-5222-45f1-9831-1846a028193b",
          "totalbeløp": 500000.0,
          "innrapporterteInntekter": [
            {
              "arbeidsgiverreferanse": "000000000",
              "inntekter": [
                {
                  "årMåned": "2018-01",
                  "beløp": 10000.0
                },
                {
                  "årMåned": "2018-02",
                  "beløp": 10000.0
                }
              ]
            }
          ]
        },
        "avviksprosent": 25.0
        }
    }"""

    @Language("JSON")
    private fun eventUtenSammenligningsgrunnlag() =
        """
    {
      "@event_name": "avvik_vurdert",
      "fødselsnummer": "12345678910",
      "aktørId": "11111100000",
      "skjæringstidspunkt": "2018-01-01",
      "avviksvurdering": {
        "id": "cc3af2c3-fa9e-4d84-a57e-a7972226cdae",
        "opprettet": "2018-01-01T00:00:00.000",
        "beregningsgrunnlag": {
          "totalbeløp": 550000.0,
          "omregnedeÅrsinntekter": [
            {
              "arbeidsgiverreferanse": "000000000",
              "beløp": 250000.0
            },
            {
              "arbeidsgiverreferanse": "000000000",
              "beløp": 300000.0
            }
          ]
        },
        "avviksprosent": 25.0
        }
    }"""
}
