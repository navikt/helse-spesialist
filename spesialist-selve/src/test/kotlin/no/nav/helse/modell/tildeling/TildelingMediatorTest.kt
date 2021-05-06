package no.nav.helse.modell.tildeling

import io.ktor.http.*
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.feilhåndtering.ModellFeil
import no.nav.helse.modell.feilhåndtering.OppgaveErAlleredeTildelt
import no.nav.helse.saksbehandler.SaksbehandlerDao
import no.nav.helse.tildeling.TildelingApiDto
import no.nav.helse.tildeling.TildelingDao
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import kotlin.test.assertEquals

internal class TildelingMediatorTest {

    companion object {
        val navn = "Sara Saksbehandler"
        val epost = "sara.saksbehandler@nav.no"
        val oid = UUID.randomUUID()
    }

    private val saksbehandlerDao = mockk<SaksbehandlerDao>(relaxed = true)
    private val tildelingDao = mockk<TildelingDao>()
    private val hendelsesMediator = mockk<HendelseMediator>(relaxed = true)
    private val tildelingMediator = TildelingMediator(saksbehandlerDao, tildelingDao, hendelsesMediator)

    @Test
    fun `stopper tildeling av allerede tildelt sak`() {
        val eksisterendeTildeling = TildelingApiDto(epost = "epost@nav.no", oid = UUID.randomUUID(), påVent = false, navn = "annen saksbehandler")
        every { tildelingDao.tildelingForOppgave(any()) } returns eksisterendeTildeling

        val feil = assertThrows<ModellFeil> {
            tildelingMediator.tildelOppgaveTilSaksbehandler(
                oppgaveId = 1L,
                saksbehandlerreferanse = eksisterendeTildeling.oid,
                epostadresse = eksisterendeTildeling.epost,
                navn = "navn"
            )
        }

        assertEquals(HttpStatusCode.Conflict, feil.httpKode())
        assertEquals(eksisterendeTildeling, feil.feil.eksternKontekst["tildeling"])
        assertEquals(eksisterendeTildeling.navn, feil.feil.eksternKontekst["tildeltTil"])
        assertEquals(OppgaveErAlleredeTildelt(eksisterendeTildeling), feil.feil)
    }
}
