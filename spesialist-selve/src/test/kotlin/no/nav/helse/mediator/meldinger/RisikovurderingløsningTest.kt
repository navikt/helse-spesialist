package no.nav.helse.mediator.meldinger

import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class RisikovurderingløsningTest {

    @Test
    fun `Oppretter arbeidsuførhetsmelding fra risikovurderingsløsning`() {
        val hendelseId = UUID.randomUUID()
        val løsningNode = objectMapper.readTree(løsningJson)
        val løsning = Risikovurderingløsning(hendelseId, UUID.randomUUID(), LocalDateTime.now(), false, løsningNode)

        val expected = """
            Arbeidsuførhet, aktivitetsplikt og/eller medvirkning må vurderes.
            Veilederen har stanset automatisk behandling (årsak: medisinsk vilkår). Se Gosys for mer informasjon. Symptomdiagnose ved 15 uker. Vurder arbeidsuførhet.
        """.trimIndent()

        assertEquals(expected, løsning.arbeidsuførhetsmelding())
    }

    @Language("JSON")
    val løsningJson = """
            {
              "@id": "${UUID.randomUUID()}",
              "@opprettet": "${LocalDateTime.now()}",
              "vedtaksperiodeId": "${UUID.randomUUID()}",
              "samletScore": 5.0,
              "begrunnelser": [
                "Veilederen har stanset automatisk behandling (årsak: medisinsk vilkår). Se Gosys for mer informasjon.",
                "Symptomdiagnose ved 15 uker. Vurder arbeidsuførhet."
              ],
              "begrunnelserSomAleneKreverManuellBehandling": [
                "Veilederen har stanset automatisk behandling (årsak: medisinsk vilkår). Se Gosys for mer informasjon.",
                "Symptomdiagnose ved 15 uker. Vurder arbeidsuførhet."
              ],
              "ufullstendig": false,
              "funn": [
                {
                  "kreverSupersaksbehandler": false,
                  "beskrivelse": "Veilederen har stanset automatisk behandling (årsak: medisinsk vilkår). Se Gosys for mer informasjon.",
                  "kategori": ["8-4"]
                },
                {
                  "kreverSupersaksbehandler": false,
                  "beskrivelse": "Symptomdiagnose ved 15 uker. Vurder arbeidsuførhet.",
                  "kategori": ["8-4"]
                }
              ],
              "kontrollertOk": [
                {
                  "beskrivelse": "Har ikke Z-diagnose som bi-diagnose",
                  "kategori": ["8-4"]
                },
                {
                  "beskrivelse": "Ikke registrert at enhet er konkurs eller under avvikling",
                  "kategori": []
                }
              ],
              "kanGodkjennesAutomatisk": false
            }
        """.trimIndent()

}
