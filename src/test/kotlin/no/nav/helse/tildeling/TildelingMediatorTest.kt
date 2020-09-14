package no.nav.helse.tildeling

import io.ktor.http.*
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.modell.feilhåndtering.ModellFeil
import no.nav.helse.modell.feilhåndtering.OppgaveErAlleredeTildelt
import no.nav.helse.modell.saksbehandler.SaksbehandlerDao
import org.junit.jupiter.api.*
import java.util.*
import kotlin.properties.Delegates
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TildelingMediatorTest {
    private val saksbehandlerDao = mockk<SaksbehandlerDao>(relaxed = true)
    private val tildelingDao = mockk<TildelingDao>(relaxed = true)
    private val tildelingMediator = TildelingMediator(saksbehandlerDao, tildelingDao)

    @Test
    fun `stopper tildeling av allerede tildelt sak`() {
        val enAnnenSaksbehandler = "enAnnenSaksbehandler"
        val saksbehandleroid = UUID.randomUUID()
        val epost = "sara.saksbehandler@nav.no"
        val navn = "Sara Saksbehandler"

        every { tildelingDao.finnSaksbehandlerNavn(any()) } returns enAnnenSaksbehandler

        val feil = assertThrows<ModellFeil> {
            tildelingMediator.tildelOppgaveTilSaksbehandler(1L, saksbehandleroid, epost, navn)
        }
        assertEquals(HttpStatusCode.Conflict, feil.httpKode())
        assertEquals(OppgaveErAlleredeTildelt(enAnnenSaksbehandler), feil.feil)
        assertEquals(enAnnenSaksbehandler, feil.feil.eksternKontekst["tildeltTil"])
    }
}
