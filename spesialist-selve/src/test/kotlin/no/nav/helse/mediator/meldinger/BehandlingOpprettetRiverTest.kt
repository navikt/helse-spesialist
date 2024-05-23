package no.nav.helse.mediator.meldinger

import io.mockk.called
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.medRivers
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.vedtaksperiode.BehandlingOpprettet
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.test.TestPerson
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class BehandlingOpprettetRiverTest {

    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val rapid = TestRapid().medRivers(BehandlingOpprettetRiver(mediator))
    private val testperson = TestPerson()

    @BeforeEach
    fun setup() {
        rapid.reset()
        clearMocks(mediator)
    }

    @Test
    fun `behandler behandling_opprettet`() {
        rapid.sendTestMessage(
            Testmeldingfabrikk.lagBehandlingOpprettet(
                aktørId = testperson.aktørId,
                fødselsnummer = testperson.fødselsnummer,
                organisasjonsnummer = testperson.orgnummer,
                vedtaksperiodeId = UUID.randomUUID(),
                spleisBehandlingId = UUID.randomUUID()
            )
        )
        verify(exactly = 1) { mediator.mottaMelding(any<BehandlingOpprettet>(), any()) }
    }

    @Test
    fun `ignorerer organisasjonsnummer ARBEIDSLEDIG uten å tryne`() {
        rapid.sendTestMessage(
            Testmeldingfabrikk.lagBehandlingOpprettet(
                aktørId = testperson.aktørId,
                fødselsnummer = testperson.fødselsnummer,
                organisasjonsnummer = "ARBEIDSLEDIG",
                vedtaksperiodeId = UUID.randomUUID(),
                spleisBehandlingId = UUID.randomUUID()
            )
        )
        verify { mediator wasNot called }
    }

    @Test
    fun `ignorerer organisasjonsnummer FRILANS uten å tryne`() {
        rapid.sendTestMessage(
            Testmeldingfabrikk.lagBehandlingOpprettet(
                aktørId = testperson.aktørId,
                fødselsnummer = testperson.fødselsnummer,
                organisasjonsnummer = "FRILANS",
                vedtaksperiodeId = UUID.randomUUID(),
                spleisBehandlingId = UUID.randomUUID()
            )
        )
        verify { mediator wasNot called }
    }

    @Test
    fun `ignorerer organisasjonsnummer SELVSTENDIG uten å tryne`() {
        rapid.sendTestMessage(
            Testmeldingfabrikk.lagBehandlingOpprettet(
                aktørId = testperson.aktørId,
                fødselsnummer = testperson.fødselsnummer,
                organisasjonsnummer = "SELVSTENDIG",
                vedtaksperiodeId = UUID.randomUUID(),
                spleisBehandlingId = UUID.randomUUID()
            )
        )
        verify { mediator wasNot called }
    }
}
