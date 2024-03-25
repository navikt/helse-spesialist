package no.nav.helse.mediator.meldinger

import io.mockk.called
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.Testdata.ORGNR
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.vedtaksperiode.BehandlingOpprettet
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class BehandlingOpprettetRiverTest {

    private val rapid = TestRapid()
    private val mediator = mockk<MeldingMediator>(relaxed = true)

    init {
        BehandlingOpprettetRiver(rapid, mediator)
    }

    @BeforeEach
    fun setup() {
        rapid.reset()
        clearMocks(mediator)
    }

    @Test
    fun `behandler behandling_opprettet`() {
        rapid.sendTestMessage(
            Testmeldingfabrikk.lagBehandlingOpprettet(
                aktørId = AKTØR,
                fødselsnummer = FØDSELSNUMMER,
                organisasjonsnummer = ORGNR,
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
                aktørId = AKTØR,
                fødselsnummer = FØDSELSNUMMER,
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
                aktørId = AKTØR,
                fødselsnummer = FØDSELSNUMMER,
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
                aktørId = AKTØR,
                fødselsnummer = FØDSELSNUMMER,
                organisasjonsnummer = "SELVSTENDIG",
                vedtaksperiodeId = UUID.randomUUID(),
                spleisBehandlingId = UUID.randomUUID()
            )
        )
        verify { mediator wasNot called }
    }
}
