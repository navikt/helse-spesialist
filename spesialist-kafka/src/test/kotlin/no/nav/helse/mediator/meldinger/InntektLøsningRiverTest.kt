package no.nav.helse.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.kafka.InntektLøsningRiver
import no.nav.helse.medRivers
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.spesialist.kafka.testfixtures.Testmeldingfabrikk
import no.nav.helse.spesialist.test.TestPerson
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class InntektLøsningRiverTest {
    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val rapid = TestRapid().medRivers(InntektLøsningRiver(mediator))
    private val testperson = TestPerson()

    @BeforeEach
    fun setup() {
        rapid.reset()
        clearMocks(mediator)
    }

    @Test
    fun `leser behov InntekterForSykepengegrunnlag`() {
        rapid.sendTestMessage(
            Testmeldingfabrikk.lagInntektløsning(
                aktørId = testperson.aktørId,
                fødselsnummer = testperson.fødselsnummer,
                orgnummer = testperson.orgnummer,
            ),
        )
        verify(exactly = 1) { mediator.løsning(any(), any(), any(), any(), any()) }
    }
}
