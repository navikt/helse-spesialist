package no.nav.helse.modell.totrinnsvurdering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.HistorikkinnslagRepository
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.mediator.oppgave.OppgaveDao
import org.junit.jupiter.api.Test
import java.util.UUID

class TotrinnsvurderingServiceTest {
    private val totrinnsvurderingDao = mockk<TotrinnsvurderingDao>(relaxed = true)

    val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val historikkinnslagRepository = mockk<HistorikkinnslagRepository>(relaxed = true)
    private val totrinnsvurderingService =
        TotrinnsvurderingService(
            totrinnsvurderingDao,
            oppgaveDao,
            historikkinnslagRepository,
        )

    @Test
    fun `Sett er retur, oppdaterer status på totrinnsvurdering og oppdaterer periodehistorikk med vedtaksperiodeId`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val oppgaveId = 42L
        val utbetalingId = UUID.randomUUID()

        every { oppgaveDao.finnIdForAktivOppgave(vedtaksperiodeId) } returns oppgaveId
        every { oppgaveDao.finnIdForAktivOppgave(any()) } returns oppgaveId
        every { oppgaveDao.finnUtbetalingId(any<Long>()) } returns utbetalingId

        totrinnsvurderingService.settAutomatiskRetur(vedtaksperiodeId)

        verify(exactly = 1) { totrinnsvurderingDao.settErRetur(vedtaksperiodeId) }
        verify(exactly = 1) { oppgaveDao.finnIdForAktivOppgave(vedtaksperiodeId) }
        verify(exactly = 1) {
            historikkinnslagRepository.lagre(
                historikkinnslag = any(),
                oppgaveId = oppgaveId,
            )
        }
    }
}
