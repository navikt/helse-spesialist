package no.nav.helse.e2e

import AbstractE2ETestV2
import ToggleHelpers.disable
import ToggleHelpers.enable
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.modell.Toggle
import org.junit.jupiter.api.Test

internal class VedtaksperiodeSkjønnsmessigFastsettelseE2ETest : AbstractE2ETestV2() {
    @Test
    fun `Prepper minimal person med personinfo`() {
        Toggle.Skjonnsfastsetting.enable()
        håndterSøknad()
        håndterVedtaksperiodeOpprettet()
        håndterVedtaksperiodeEndret(gjeldendeTilstand = "AVVENTER_SKJØNNSMESSIG_FASTSETTELSE")
        håndterPersoninfoløsning()
        assertHarPersoninfo(FØDSELSNUMMER)
        assertSaksbehandleroppgaveBleIkkeOpprettet()
        Toggle.Skjonnsfastsetting.disable()
    }
}
