package no.nav.helse.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.kafka.TilbakedateringBehandletRiver
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.kommando.TilbakedateringBehandlet
import no.nav.helse.spesialist.kafka.medRivers
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class TilbakedateringBehandletRiverTest {
    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val testRapid = TestRapid().medRivers(TilbakedateringBehandletRiver(mediator))

    @Test
    fun `Leser tilbakedatering behandlet`() {
        testRapid.sendTestMessage(event())
        verify(exactly = 1) { mediator.mottaMelding(any<TilbakedateringBehandlet>(), any()) }
    }

    @Language("JSON")
    private fun event() =
        """
    {
      "@event_name": "tilbakedatering_behandlet",
      "@id": "${UUID.randomUUID()}",
      "@opprettet": "${LocalDateTime.now()}",
      "fødselsnummer": "11111100000",
      "sykmeldingId": "${UUID.randomUUID()}",
      "syketilfelleStartDato": "${LocalDate.now()}",
      "perioder": [
          {
            "fom": "${LocalDate.now()}",
            "tom": "${LocalDate.now().plusDays(30)}"
          }
      ]
    }"""
}
