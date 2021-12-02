package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.util.*

class AdressebeskyttelseEndretRiverTest {
    private companion object {
        private val HENDELSE_ID = UUID.randomUUID()
        private const val FNR = "12345678911"
        private const val AKTØR = "1234567891234"
    }
    private val testmeldingfabrikk = Testmeldingfabrikk(FNR, AKTØR)
    private val mediator = mockk<HendelseMediator>(relaxed = true)
    private val testRapid = TestRapid().apply {
        AdressebeskyttelseEndret.AdressebeskyttelseEndretRiver(this, mediator)
    }

    @Test
    fun `leser adressebeskyttelse endret event`() {
        testRapid.sendTestMessage(testmeldingfabrikk.lagAdressebeskyttelseEndret(id = HENDELSE_ID))
        verify(exactly = 1) { mediator.adressebeskyttelseEndret(any(), HENDELSE_ID, FNR, any()) }
    }
}
