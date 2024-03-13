package no.nav.helse.mediator.meldinger

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.mediator.meldinger.løsninger.Inntektløsning
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class InntektRiverTest {

    private val rapid = TestRapid()
    private val mediator = mockk<MeldingMediator>(relaxed = true)

    init {
        Inntektløsning.InntektRiver(rapid, mediator)
    }

    @BeforeEach
    fun setup() {
        rapid.reset()
        clearMocks(mediator)
    }

    @Test
    fun `leser behov InntekterForSykepengegrunnlag`() {
        rapid.sendTestMessage(
            Testmeldingfabrikk.lagInntektløsning(aktørId = AKTØR, fødselsnummer = FØDSELSNUMMER, ORGNR)
        )
        verify(exactly = 1) { mediator.løsning(any(), any(), any(), any(), any()) }
    }
}
