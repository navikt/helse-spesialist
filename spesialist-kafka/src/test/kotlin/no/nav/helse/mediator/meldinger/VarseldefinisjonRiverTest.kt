package no.nav.helse.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.helse.kafka.VarseldefinisjonRiver
import no.nav.helse.spesialist.application.InMemoryRepositoriesAndDaos
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarseldefinisjonId
import no.nav.helse.spesialist.domain.testfixtures.lagBehandlingUnikId
import no.nav.helse.spesialist.domain.testfixtures.lagSpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.lagVarsel
import no.nav.helse.spesialist.domain.testfixtures.lagVarseldefinisjonId
import no.nav.helse.spesialist.kafka.medTransaksjonelleRivers
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class VarseldefinisjonRiverTest {
    private val inMemoryRepositoriesAndDaos = InMemoryRepositoriesAndDaos()
    private val testRapid = TestRapid().medTransaksjonelleRivers(inMemoryRepositoriesAndDaos, VarseldefinisjonRiver())

    @Test
    fun `leser definisjon fra kafka`() {
        val id = VarseldefinisjonId(UUID.randomUUID())
        testRapid.sendTestMessage(varseldefinisjon(id))
        val definisjon = inMemoryRepositoriesAndDaos.sessionContext.varseldefinisjonRepository.finnOrNull(id)
        assertNotNull(definisjon)
        assertEquals(id, definisjon.id)
        assertEquals("XX_YY_1", definisjon.kode)
        assertEquals("En tittel", definisjon.tittel)
        assertEquals("En forklaring", definisjon.forklaring)
        assertEquals("En handling", definisjon.handling)
        assertEquals(false, definisjon.avviklet)
        assertEquals(LocalDateTime.of(2023, 3, 16, 0, 0), definisjon.opprettet)
    }

    @Test
    fun `leser definisjon fra kafka uten forklaring og handling`() {
        val id = VarseldefinisjonId(UUID.randomUUID())
        testRapid.sendTestMessage(varseldefinisjonUtenForklaringOgHandling(id))
        val definisjon = inMemoryRepositoriesAndDaos.sessionContext.varseldefinisjonRepository.finnOrNull(id)
        assertNotNull(definisjon)
        assertEquals(id, definisjon.id)
        assertEquals("XX_YY_1", definisjon.kode)
        assertEquals("En tittel", definisjon.tittel)
        assertNull(definisjon.forklaring)
        assertNull(definisjon.handling)
        assertEquals(false, definisjon.avviklet)
        assertEquals(LocalDateTime.of(2023, 3, 16, 0, 0), definisjon.opprettet)
    }

    @Test
    fun `alle varsler som tilhører koden blir avviklet hvis koden er avviklet`() {
        // given
        val varsel =
            lagVarsel(
                kode = "XX_YY_1",
                behandlingUnikId = lagBehandlingUnikId(),
                spleisBehandlingId = lagSpleisBehandlingId(),
            )
        inMemoryRepositoriesAndDaos.sessionContext.varselRepository.lagre(varsel)

        // when
        testRapid.sendTestMessage(varseldefinisjon(avviklet = true))

        // then
        val avvikletVarsel = inMemoryRepositoriesAndDaos.sessionContext.varselRepository.finnOrNull(varsel.id)
        assertNotNull(avvikletVarsel)
        assertEquals(Varsel.Status.AVVIKLET, avvikletVarsel.status)
    }

    @Language("JSON")
    private fun varseldefinisjon(
        id: VarseldefinisjonId = lagVarseldefinisjonId(),
        avviklet: Boolean = false,
    ) = """
    {
      "@event_name": "varselkode_ny_definisjon",
      "varselkode": "XX_YY_1",
      "gjeldende_definisjon": {
        "id": "${id.value}",
        "kode": "XX_YY_1",
        "tittel": "En tittel",
        "forklaring": "En forklaring",
        "handling": "En handling",
        "avviklet": $avviklet,
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
    private fun varseldefinisjonUtenForklaringOgHandling(id: VarseldefinisjonId) =
        """
    {
      "@event_name": "varselkode_ny_definisjon",
      "varselkode": "XX_YY_1",
      "gjeldende_definisjon": {
        "id": "${id.value}",
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
