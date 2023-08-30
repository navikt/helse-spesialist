package no.nav.helse.modell.tildeling

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingMediator
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerDao
import no.nav.helse.spesialist.api.tildeling.TildelingDao
import no.nav.helse.utenNoenTilganger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TildelingServiceTest {

    companion object {
        const val navn = "Sara Saksbehandler"
        const val epost = "sara.saksbehandler@nav.no"
        val SAKSBEHANDLER = UUID.randomUUID()
        const val oppgaveref = 1L
    }

    private val saksbehandlerDao = mockk<SaksbehandlerDao>(relaxed = true)
    private val tildelingDao = mockk<TildelingDao>()
    private val hendelseMediator = mockk<HendelseMediator>(relaxed = true)
    private val totrinnsvurderingMediator = mockk<TotrinnsvurderingMediator>(relaxed = true)
    private val tildelingService = TildelingService(
        tildelingDao,
        hendelseMediator,
        totrinnsvurderingMediator,
    )

    @BeforeEach
    fun setup() {
        clearMocks(saksbehandlerDao, tildelingDao, hendelseMediator, totrinnsvurderingMediator)
    }

    @Test
    fun `hvis det finnes en tidligere_saksbehandler blir tildeling fjernet og tidligere saksbehandler tildelt`() {
        every { tildelingDao.slettTildeling(oppgaveref) } returns 1
        every { tildelingDao.tildelingForOppgave(any()) } returns null
        every { hendelseMediator.tildelOppgaveTilSaksbehandler(any(), any()) } returns true

        tildelingService.fjernTildelingOgTildelNySaksbehandlerHvisFinnes(oppgaveref, SAKSBEHANDLER, utenNoenTilganger())

        verify(exactly = 1) { tildelingDao.slettTildeling(oppgaveref) }
        verify(exactly = 1) { hendelseMediator.tildelOppgaveTilSaksbehandler(oppgaveref, SAKSBEHANDLER) }
    }

    @Test
    fun `hvis det ikke finnes en tidligere_saksbehandler blir tildeling fjernet`() {
        every { tildelingDao.slettTildeling(oppgaveref) } returns 1
        every { tildelingDao.tildelingForOppgave(any()) } returns null
        every { hendelseMediator.tildelOppgaveTilSaksbehandler(any(), any()) } returns true

        tildelingService.fjernTildelingOgTildelNySaksbehandlerHvisFinnes(oppgaveref, null, utenNoenTilganger())

        verify(exactly = 1) { tildelingDao.slettTildeling(oppgaveref) }
        verify(exactly = 0) { hendelseMediator.tildelOppgaveTilSaksbehandler(any(), any()) }
    }
}
