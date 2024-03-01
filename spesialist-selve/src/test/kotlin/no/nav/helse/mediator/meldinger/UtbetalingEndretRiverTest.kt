package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.utbetaling.UtbetalingEndret
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class UtbetalingEndretRiverTest {

    private val mediator = mockk<HendelseMediator>(relaxed = true)
    private val testRapid = TestRapid()

    init {
        UtbetalingEndretRiver(testRapid, mediator)
    }

    @Test
    fun `Leser inn utbetaling_endret-event`() {
        testRapid.sendTestMessage(event())
        verify(exactly = 1) { mediator.utbetalingEndret(any<UtbetalingEndret>(), any(), any()) }
    }

    @Test
    fun `Feiler dersom vi ikke forstår utbetaling-status`() {
        testRapid.sendTestMessage(event("Noe rart med sukker på"))
        verify(exactly = 0) { mediator.utbetalingEndret(any(), any(), any()) }
    }

    @Language("JSON")
    private fun event(gjeldendeStatus: String = "UTBETALT") = """
    {
      "utbetalingId": "${UUID.randomUUID()}",
      "type": "UTBETALING",
      "forrigeStatus": "OVERFØRT",
      "gjeldendeStatus": "$gjeldendeStatus",
      "arbeidsgiverOppdrag": {
        "mottaker": "987654321",
        "fagområde": "SPREF",
        "linjer": [
          {
            "fom": "2021-01-01",
            "tom": "2021-01-31",
            "dagsats": 1000,
            "lønn": 9999,
            "grad": 100,
            "stønadsdager": 31,
            "totalbeløp": 31000,
            "endringskode": "NY",
            "delytelseId": 1,
            "refDelytelseId": null,
            "refFagsystemId": null,
            "statuskode": null,
            "datoStatusFom": null,
            "klassekode": "SPREFAG-IOP"
          }
        ],
        "fagsystemId": "EN_FAGSYSTEMID",
        "endringskode": "ENDR",
        "sisteArbeidsgiverdag": null,
        "tidsstempel": "${LocalDateTime.now()}",
        "nettoBeløp": 31000,
        "stønadsdager": 31,
        "fom": "2021-01-01",
        "tom": "2021-01-31"
      },
      "personOppdrag": {
        "mottaker": "987654321",
        "fagområde": "SP",
        "linjer": [],
        "fagsystemId": "EN_ANNEN_FAGSYSTEMID",
        "endringskode": "NY",
        "sisteArbeidsgiverdag": null,
        "tidsstempel": "${LocalDateTime.now()}",
        "nettoBeløp": 0,
        "stønadsdager": 0,
        "fom": "-999999999-01-01",
        "tom": "-999999999-01-01"
      },
      "@event_name": "utbetaling_endret",
      "@id": "${UUID.randomUUID()}",
      "@opprettet": "${LocalDateTime.now()}",
      "aktørId": "1111100000000",
      "fødselsnummer": "11111100000",
      "organisasjonsnummer": "987654321"
    }
"""
}
