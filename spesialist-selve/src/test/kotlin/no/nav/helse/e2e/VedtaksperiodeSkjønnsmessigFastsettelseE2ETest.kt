package no.nav.helse.e2e

import AbstractE2ETest
import no.nav.helse.Testdata.FØDSELSNUMMER
import org.junit.jupiter.api.Test

internal class VedtaksperiodeSkjønnsmessigFastsettelseE2ETest : AbstractE2ETest() {
    @Test
    fun `Prepper minimal person med personinfo`() {
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterVedtaksperiodeEndret(gjeldendeTilstand = "AVVENTER_SKJØNNSMESSIG_FASTSETTELSE")
        håndterPersoninfoløsning()
        assertHarPersoninfo(FØDSELSNUMMER)
        assertSaksbehandleroppgaveBleIkkeOpprettet()
    }
}
