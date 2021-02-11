package no.nav.helse.modell.tildeling

import io.ktor.http.*
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.feilhåndtering.ModellFeil
import no.nav.helse.modell.feilhåndtering.OppgaveErAlleredeTildelt
import no.nav.helse.modell.saksbehandler.SaksbehandlerDao
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import kotlin.test.assertEquals

internal class TildelingMediatorTest {
    private val saksbehandlerDao = mockk<SaksbehandlerDao>(relaxed = true)
    private val tildelingDao = mockk<TildelingDao>(relaxed = true)
    private val hendelsesMediator = mockk<HendelseMediator>(relaxed = true)
    private val tildelingMediator = TildelingMediator(saksbehandlerDao, tildelingDao, hendelsesMediator)

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
