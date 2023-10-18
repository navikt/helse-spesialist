package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.mediator.meldinger.løsninger.DokumentRiver
import no.nav.helse.modell.dokument.DokumentDao
import no.nav.helse.objectMapper
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DokumentRiverTest {
    private companion object {
        private const val FNR = "12345678911"
        private val DOKUMENTID = UUID.randomUUID()
    }

    private val dokumentDao = mockk<DokumentDao>(relaxed = true)
    private val testRapid = TestRapid().apply {
        DokumentRiver(this, dokumentDao)
    }

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `leser HentDokument-melding`() {
        testRapid.sendTestMessage(Testmeldingfabrikk.lagHentDokumentLøsning(FNR, DOKUMENTID))
        verify(exactly = 1) { dokumentDao.lagre(FNR, DOKUMENTID, objectMapper.createObjectNode()) }
    }
}