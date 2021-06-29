package no.nav.helse.modell.tildeling

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.feilhåndtering.OppgaveAlleredeTildelt
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.saksbehandler.SaksbehandlerDao
import no.nav.helse.tildeling.TildelingApiDto
import no.nav.helse.tildeling.TildelingDao
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

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

        assertThrows<OppgaveAlleredeTildelt> {
            tildelingMediator.tildelOppgaveTilSaksbehandler(
                oppgaveId = 1L,
                saksbehandlerreferanse = eksisterendeTildeling.oid,
                epostadresse = eksisterendeTildeling.epost,
                navn = "navn",
                ident = "Z999999"
            )
        }
    }
}
