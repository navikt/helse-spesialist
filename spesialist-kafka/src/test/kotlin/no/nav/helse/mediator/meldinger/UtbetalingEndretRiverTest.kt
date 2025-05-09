package no.nav.helse.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.kafka.UtbetalingEndretRiver
import no.nav.helse.medRivers
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.utbetaling.UtbetalingEndret
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID

internal class UtbetalingEndretRiverTest {

    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val testRapid = TestRapid().medRivers(UtbetalingEndretRiver(mediator))

    @Test
    fun `Leser inn utbetaling_endret-event`() {
        testRapid.sendTestMessage(event())
        verify(exactly = 1) { mediator.mottaMelding(any<UtbetalingEndret>(), any()) }
    }

    @Test
    fun `Feiler dersom vi ikke forstår utbetaling-status`() {
        assertThrows<IllegalStateException> {
            testRapid.sendTestMessage(event("Noe rart med sukker på"))
        }
        verify(exactly = 0) { mediator.mottaMelding(any(), any()) }
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
