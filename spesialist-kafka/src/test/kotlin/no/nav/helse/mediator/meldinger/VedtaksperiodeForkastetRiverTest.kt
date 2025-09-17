package no.nav.helse.mediator.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.kafka.VedtaksperiodeForkastetRiver
import no.nav.helse.spesialist.kafka.medRivers
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeForkastet
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.kafka.testfixtures.Testmeldingfabrikk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

internal class VedtaksperiodeForkastetRiverTest {

    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val rapid = TestRapid().medRivers(VedtaksperiodeForkastetRiver(mediator))

    @Test
    fun `tar imot forkastet-message`() {
        val fødselsnummer = lagFødselsnummer()
        val vedtaksperiodeId = UUID.randomUUID()
        rapid.sendTestMessage(Testmeldingfabrikk.lagVedtaksperiodeForkastet("aktørId", fødselsnummer, vedtaksperiodeId))
        verify(exactly = 1) {
            mediator.mottaMelding(
                melding = withArg<VedtaksperiodeForkastet> {
                    assertEquals(fødselsnummer, it.fødselsnummer())
                    assertEquals(vedtaksperiodeId, it.vedtaksperiodeId())
                },
                kontekstbasertPubliserer = any()
            )
        }
    }
}
