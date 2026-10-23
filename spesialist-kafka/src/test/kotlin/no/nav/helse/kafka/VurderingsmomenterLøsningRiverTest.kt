package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.meldinger.løsninger.Risikovurderingløsning
import no.nav.helse.spesialist.kafka.medRivers
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class VurderingsmomenterLøsningRiverTest {
    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val testRapid = TestRapid().medRivers(VurderingsmomenterLøsningRiver(mediator))

    private val hendelseId = UUID.randomUUID()
    private val contextId = UUID.randomUUID()
    private val id = UUID.randomUUID()
    private val vedtaksperiodeId = UUID.randomUUID()
    private val annenVedtaksperiodeId = UUID.randomUUID()

    @Test
    fun `løsning uten andreVedtaksperioder gir tom liste`() {
        testRapid.sendTestMessage(løsning(andreVedtaksperioder = null))

        val løsning = mottattLøsning()
        assertFalse(løsning.kanGodkjennesAutomatisk)
        assertEquals(emptyList<Risikovurderingløsning.AnnenVedtaksperiode>(), løsning.andreVedtaksperioder)
    }

    @Test
    fun `leser risikovurderinger av andre vedtaksperioder`() {
        testRapid.sendTestMessage(
            løsning(
                andreVedtaksperioder =
                    """
                    [
                      { "vedtaksperiodeId": "$annenVedtaksperiodeId", "kanGodkjennesAutomatisk": false, "funn": [], "kontrollertOk": [] }
                    ]
                    """.trimIndent(),
            ),
        )

        val annen = mottattLøsning().andreVedtaksperioder.single()
        assertEquals(annenVedtaksperiodeId, annen.vedtaksperiodeId)
        assertFalse(annen.kanGodkjennesAutomatisk)
    }

    @Test
    fun `ignorerer ugyldige elementer i andreVedtaksperioder`() {
        testRapid.sendTestMessage(
            løsning(
                andreVedtaksperioder =
                    """
                    [
                      { "vedtaksperiodeId": "ikke-en-uuid", "kanGodkjennesAutomatisk": false, "funn": [], "kontrollertOk": [] },
                      { "vedtaksperiodeId": "$annenVedtaksperiodeId", "funn": [], "kontrollertOk": [] },
                      { "vedtaksperiodeId": "$annenVedtaksperiodeId", "kanGodkjennesAutomatisk": false },
                      { "vedtaksperiodeId": "$annenVedtaksperiodeId", "kanGodkjennesAutomatisk": true, "funn": [], "kontrollertOk": [] }
                    ]
                    """.trimIndent(),
            ),
        )

        val annen = mottattLøsning().andreVedtaksperioder.single()
        assertEquals(annenVedtaksperiodeId, annen.vedtaksperiodeId)
        assertTrue(annen.kanGodkjennesAutomatisk)
    }

    private fun mottattLøsning(): Risikovurderingløsning {
        val løsning = slot<Risikovurderingløsning>()
        verify(exactly = 1) { mediator.løsning(hendelseId, contextId, id, capture(løsning), any(), any()) }
        return løsning.captured
    }

    @Language("JSON")
    private fun løsning(andreVedtaksperioder: String?): String =
        """
        {
          "@event_name": "behov",
          "@behov": ["Risikovurdering"],
          "@final": true,
          "fødselsnummer": "12345678910",
          "hendelseId": "$hendelseId",
          "contextId": "$contextId",
          "sti": [1],
          "Risikovurdering": {
            "vedtaksperiodeId": "$vedtaksperiodeId"
          },
          "@id": "$id",
          "@opprettet": "2018-01-01T00:00:00.000",
          "@løsning": {
            "Risikovurdering": {
              "kanGodkjennesAutomatisk": false,
              "funn": [],
              "kontrollertOk": []${andreVedtaksperioder?.let { ",\n\"andreVedtaksperioder\": $it" } ?: ""}
            }
          }
        }
        """.trimIndent()
}
