package no.nav.helse.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.kafka.DokumentRiver
import no.nav.helse.medRivers
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.spesialist.kafka.objectMapper
import no.nav.helse.spesialist.kafka.testfixtures.Testmeldingfabrikk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class DokumentRiverTest {
    private companion object {
        private const val FNR = "12345678911"
        private val DOKUMENTID = UUID.randomUUID()
    }

    private val meldingMediator = mockk<MeldingMediator>(relaxed = true)
    private val testRapid = TestRapid().medRivers(DokumentRiver(meldingMediator))

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `leser HentDokument-melding`() {
        testRapid.sendTestMessage(Testmeldingfabrikk.lagHentDokumentLøsning(FNR, DOKUMENTID))
        verify(exactly = 1) { meldingMediator.mottaDokument(FNR, DOKUMENTID, objectMapper.createObjectNode()) }
    }
}
