package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import lagFødselsnummer
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.person.EndretEgenAnsattStatus
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test

internal class EndretSkjermetinfoRiverTest {

    private val testRapid = TestRapid()
    private val mediator = mockk<MeldingMediator>(relaxed = true)

    init {
        EndretSkjermetinfoRiver(testRapid, mediator)
    }

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