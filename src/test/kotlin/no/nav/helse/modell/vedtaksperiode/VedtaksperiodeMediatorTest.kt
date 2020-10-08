package no.nav.helse.modell.vedtaksperiode

import AbstractE2ETest
import io.mockk.every
import no.nav.helse.rapids_rivers.isMissingOrNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class VedtaksperiodeMediatorTest : AbstractE2ETest() {
    private companion object {
        private val VEDTAKSPERIODE_ID = UUID.randomUUID()
        private const val FØDSELSNUMMER = "12020052345"
        private const val AKTØR = "999999999"
        private const val ORGNR = "222222222"
        private val ID = UUID.randomUUID()
    }

    @BeforeEach
    fun setup() {
        every { restClient.hentSpeilSpapshot(FØDSELSNUMMER) } returns SNAPSHOTV1
    }

    @AfterEach
    fun tearDown() {
        every { miljøstyrtFeatureToggle.risikovurdering() }.returns(false)
    }

    @Test
    fun `manglende risikovurdering mappes ikke til speil`() {
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        val speilSnapshot = requireNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER))

        assertTrue(
            speilSnapshot.arbeidsgivere.first().vedtaksperioder.first().path("risikovurdering").isMissingOrNull()
        )
    }

    @Test
    fun `En satt risikovurdering mappes til speil`() {
        every { miljøstyrtFeatureToggle.risikovurdering() }.returns(true)
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID
        )
        val speilSnapshot = requireNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER))

        val risikovurdering = speilSnapshot.arbeidsgivere.first().vedtaksperioder.first().path("risikovurdering")

        assertEquals(false, risikovurdering["ufullstendig"].booleanValue())
        assertTrue(risikovurdering["arbeidsuførhetvurdering"].isEmpty)
    }

    @Test
    fun `Warnings mappes til speil (som "varsler")`() {
        every { miljøstyrtFeatureToggle.risikovurdering() }.returns(true)
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        sendRisikovurderingløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            vedtaksperiodeId = VEDTAKSPERIODE_ID,
            begrunnelser = listOf("8-4 ikke oppfylt")
        )
        val speilSnapshot = requireNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER))

        val varsler = speilSnapshot.arbeidsgivere.first().vedtaksperioder.first().path("varsler")

        assertEquals(1, varsler.size())
    }

    @Test
    fun `Ingen warnings mappes til speil som tom liste`() {
        val godkjenningsmeldingId = sendGodkjenningsbehov(ORGNR, VEDTAKSPERIODE_ID)
        sendPersoninfoløsning(godkjenningsmeldingId, ORGNR, VEDTAKSPERIODE_ID)
        sendDigitalKontaktinformasjonløsning(
            godkjenningsmeldingId = godkjenningsmeldingId,
            erDigital = true
        )
        sendÅpneGosysOppgaverløsning(
            godkjenningsmeldingId = godkjenningsmeldingId
        )
        val speilSnapshot = requireNotNull(vedtaksperiodeMediator.byggSpeilSnapshotForFnr(FØDSELSNUMMER))

        val varsler = speilSnapshot.arbeidsgivere.first().vedtaksperioder.first().path("varsler")

        assertTrue(varsler.isEmpty)
    }

    private val SNAPSHOTV1 = """
        {
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
                    ]
                }
            ]
        }
        """.trimIndent()

}
