package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VarseldefinisjonRiverTest {
    private val testRapid = TestRapid()
    private val mediator = mockk<HendelseMediator>(relaxed = true)

    init {
        VarseldefinisjonRiver(testRapid, mediator)
    }

    @BeforeEach
    fun beforeEach() {
        testRapid.reset()
    }

    @Test
    fun `leser definisjon fra kafka`() {
        val id = UUID.fromString("ee7f8701-e70b-4752-a714-cfa76dba2f3a")
        testRapid.sendTestMessage(varseldefinisjon(id))
        verify(exactly = 1) { mediator.håndter(any()) }
    }

    @Test
    fun `leser definisjon fra kafka uten forklaring og handling`() {
        val id = UUID.fromString("ee7f8701-e70b-4752-a714-cfa76dba2f3a")
        testRapid.sendTestMessage(varseldefinisjonUtenForklaringOgHandling(id))
        verify(exactly = 1) { mediator.håndter(any()) }
    }

    @Language("JSON")
    private fun varseldefinisjon(id: UUID) = """
    {
      "@event_name": "varselkode_ny_definisjon",
      "varselkode": "XX_YY_1",
      "gjeldende_definisjon": {
        "id": "$id",
        "kode": "XX_YY_1",
        "tittel": "En tittel",
        "forklaring": "En forklaring",
        "handling": "En handling",
        "avviklet": false,
        "opprettet": "2023-03-16T00:00:00.000000"
      },
      "@id": "0993678d-dded-4edb-b032-02f668787206",
      "@opprettet": "2023-03-16T00:00:00.000000",
      "system_read_count": 0,
      "system_participating_services": [
        {
          "id": "a0dbf2f8-a107-4073-9669-69ce03e2f1fd",
          "time": "2023-03-16T00:00:00.000000"
        }
      ]
    } 
    """

    @Language("JSON")
    private fun varseldefinisjonUtenForklaringOgHandling(id: UUID) = """
    {
      "@event_name": "varselkode_ny_definisjon",
      "varselkode": "XX_YY_1",
      "gjeldende_definisjon": {
        "id": "$id",
        "kode": "XX_YY_1",
        "tittel": "En tittel",
        "forklaring": null,
        "handling": null,
        "avviklet": false,
        "opprettet": "2023-03-16T00:00:00.000000"
      },
      "@id": "0993678d-dded-4edb-b032-02f668787206",
      "@opprettet": "2023-03-16T00:00:00.000000",
      "system_read_count": 0,
      "system_participating_services": [
        {
          "id": "a0dbf2f8-a107-4073-9669-69ce03e2f1fd",
          "time": "2023-03-16T00:00:00.000000"
        }
      ]
    } 
    """
}