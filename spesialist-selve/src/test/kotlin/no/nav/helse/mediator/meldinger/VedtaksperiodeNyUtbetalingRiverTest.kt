package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.medRivers
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeNyUtbetaling
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.test.TestPerson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class VedtaksperiodeNyUtbetalingRiverTest {
    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val testRapid = TestRapid().medRivers(VedtaksperiodeNyUtbetalingRiver(mediator))
    private val testperson = TestPerson()

    @BeforeEach
    fun setUp() {
        testRapid.reset()
    }

    @Test
    fun `leser inn vedtaksperiode_ny_utbetaling`() {
        val hendelseId = UUID.randomUUID()
        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagVedtaksperiodeNyUtbetaling(
                id = hendelseId,
                aktørId = testperson.aktørId,
                fødselsnummer = testperson.fødselsnummer,
                organisasjonsnummer = testperson.orgnummer,
                vedtaksperiodeId = testperson.vedtaksperiodeId1,
                utbetalingId = testperson.utbetalingId1,
            ),
        )
        verify(exactly = 1) {
            mediator.mottaMelding(
                melding =
                    withArg<VedtaksperiodeNyUtbetaling> {
                        assertEquals(hendelseId, it.id)
                        assertEquals(testperson.fødselsnummer, it.fødselsnummer())
                        assertEquals(testperson.vedtaksperiodeId1, it.vedtaksperiodeId())
                        assertEquals(testperson.utbetalingId1, it.utbetalingId)
                    },
                messageContext = any(),
            )
        }
    }
}
