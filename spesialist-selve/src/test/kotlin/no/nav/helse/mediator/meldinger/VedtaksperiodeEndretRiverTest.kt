package no.nav.helse.mediator.meldinger

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.medRivers
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeEndret
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.test.TestPerson
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class VedtaksperiodeEndretRiverTest {
    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val rapid = TestRapid().medRivers(VedtaksperiodeEndretRiver(mediator))
    private val testperson = TestPerson()

    @BeforeEach
    fun setup() {
        rapid.reset()
        clearMocks(mediator)
    }

    @Test
    fun `leser vedtaksperiode_endret så lenge gjeldende tilstand er AVSLUTTET`() {
        rapid.sendTestMessage(
            Testmeldingfabrikk.lagVedtaksperiodeEndret(
                aktørId = testperson.aktørId,
                fødselsnummer = testperson.fødselsnummer,
                gjeldendeTilstand = "AVSLUTTET",
            ),
        )
        verify(exactly = 1) { mediator.mottaMelding(any<VedtaksperiodeEndret>(), messageContext = any()) }
    }

    @Test
    fun `leser ikke vedtaksperiode_endret så lenge gjeldende tilstand ikke er AVSLUTTET`() {
        rapid.sendTestMessage(
            Testmeldingfabrikk.lagVedtaksperiodeEndret(
                aktørId = testperson.aktørId,
                fødselsnummer = testperson.fødselsnummer,
                gjeldendeTilstand = "NOE ANNET",
            ),
        )
        verify(exactly = 0) { mediator.mottaMelding(any<VedtaksperiodeEndret>(), messageContext = any()) }
    }
}
