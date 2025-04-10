package no.nav.helse.e2e

import no.nav.helse.TestRapidHelpers.behov
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KommandokjedePåminnelseE2ETest : AbstractE2ETest() {

    @Test
    fun `påminnelse sender ut nytt behov`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterGodkjenningsbehov()

        håndterKommandokjedePåminnelse(commandContextId(sisteGodkjenningsbehovId), sisteGodkjenningsbehovId)
        assertEquals(3, inspektør.behov().size)
    }
}
