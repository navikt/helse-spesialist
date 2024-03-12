package no.nav.helse.e2e

import AbstractE2ETest
import no.nav.helse.TestRapidHelpers.behov
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class KommandokjedePåminnelseE2ETest : AbstractE2ETest() {

    @Test
    fun `påminnelse sender ut nytt behov`() {
        håndterSøknad()
        spleisOppretterNyBehandling()
        håndterGodkjenningsbehov()

        håndterKommandokjedePåminnelse(commandContextId(sisteGodkjenningsbehovId), sisteGodkjenningsbehovId)
        assertEquals(2, inspektør.behov().size)
    }
}
