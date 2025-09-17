package no.nav.helse.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.kafka.AdressebeskyttelseEndretRiver
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.spesialist.kafka.medRivers
import no.nav.helse.spesialist.kafka.testfixtures.Testmeldingfabrikk
import org.junit.jupiter.api.Test
import java.util.UUID

class AdressebeskyttelseEndretRiverTest {
    private companion object {
        private val HENDELSE_ID = UUID.randomUUID()
        private const val FNR = "12345678911"
        private const val AKTØR = "1234567891234"
    }

    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val testRapid = TestRapid().medRivers(AdressebeskyttelseEndretRiver(mediator))

    @Test
    fun `leser adressebeskyttelse endret event`() {
        testRapid.sendTestMessage(Testmeldingfabrikk.lagAdressebeskyttelseEndret(AKTØR, FNR, HENDELSE_ID))
        verify(exactly = 1) { mediator.mottaMelding(any<AdressebeskyttelseEndret>(), any()) }
    }
}
