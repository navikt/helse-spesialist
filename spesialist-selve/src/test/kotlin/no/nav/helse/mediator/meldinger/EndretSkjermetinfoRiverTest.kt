package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.medRivers
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.person.EndretEgenAnsattStatus
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.test.lagFødselsnummer
import org.junit.jupiter.api.Test
import java.util.UUID

internal class EndretSkjermetinfoRiverTest {

    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val testRapid = TestRapid().medRivers(EndretSkjermetinfoRiver(mediator))

    @Test
    fun `leser endret_skjermetinfo`() {
        testRapid.sendTestMessage(Testmeldingfabrikk.lagEndretSkjermetinfo(lagFødselsnummer(), true, UUID.randomUUID()))
        verify(exactly = 1) { mediator.mottaMelding(any<EndretEgenAnsattStatus>(), any()) }
    }
    @Test
    fun `leser ikke endret_skjermetinfo hvis fødselsnummer ikke er gyldig`() {
        testRapid.sendTestMessage(Testmeldingfabrikk.lagEndretSkjermetinfo("ugyldig", true, UUID.randomUUID()))
        verify(exactly = 0) { mediator.mottaMelding(any(), any()) }
    }
}
