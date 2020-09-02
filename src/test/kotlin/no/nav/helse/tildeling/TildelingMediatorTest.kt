package no.nav.helse.tildeling

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals

class TildelingMediatorTest {

    @Test
    @Disabled
    fun `lagre en tildeling`() {
        val tildelingMediator = TildelingMediator()
        val oppgavereferanse = UUID.randomUUID()
        val saksbehandlerReferanse = UUID.randomUUID()

        tildelingMediator.tildelOppgaveTilSaksbehandler(oppgavereferanse, saksbehandlerReferanse)

        assertEquals(saksbehandlerReferanse, tildelingMediator.hentSaksbehandlerFor(oppgavereferanse))
    }
}
