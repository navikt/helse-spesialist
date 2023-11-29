package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class AvviksvurderingRiverTest {

    private val testRapid = TestRapid()
    private val mediator = mockk<HendelseMediator>(relaxed = true)


    init {
        AvviksvurderingRiver(testRapid, mediator)
    }

    @Test
    fun foo() {
        testRapid.sendTestMessage(event())
    }

    @Language("JSON")
    private fun event() = """
    {
      "@event_name": "avviksvurdering",
      "fødselsnummer": "12345678910",
      "aktørId": "11111100000",
      "skjæringstidspunkt": "2018-01-01",
      "avviksvurdering": {
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
        "sammenligningsgrunnlag": {
          "totalbeløp": 500000.0,
          "innraporterteInntekter": [
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
}