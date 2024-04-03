package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.person.AdressebeskyttelseEndretRiver
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test

class AdressebeskyttelseEndretRiverTest {
    private companion object {
        private val HENDELSE_ID = UUID.randomUUID()
        private const val FNR = "12345678911"
        private const val AKTØR = "1234567891234"
    }

    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val testRapid = TestRapid().apply {
        AdressebeskyttelseEndretRiver(this, mediator)
    }

    @Test
    fun `leser adressebeskyttelse endret event`() {
        testRapid.sendTestMessage(Testmeldingfabrikk.lagAdressebeskyttelseEndret(AKTØR, FNR, HENDELSE_ID))
        verify(exactly = 1) { mediator.mottaMelding(any<AdressebeskyttelseEndret>(), any()) }
    }
}
