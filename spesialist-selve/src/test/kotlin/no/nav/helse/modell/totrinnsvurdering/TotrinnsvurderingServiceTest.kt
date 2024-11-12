package no.nav.helse.modell.totrinnsvurdering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.DialogDao
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.modell.periodehistorikk.TotrinnsvurderingAutomatiskRetur
import org.junit.jupiter.api.Test
import java.util.UUID

class TotrinnsvurderingServiceTest {
    private val totrinnsvurderingDao = mockk<TotrinnsvurderingDao>(relaxed = true)

    val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val periodehistorikkDao = mockk<PeriodehistorikkDao>(relaxed = true)
    private val dialogDao = mockk<DialogDao>(relaxed = true)
    private val totrinnsvurderingService =
        TotrinnsvurderingService(
            totrinnsvurderingDao,
            oppgaveDao,
            periodehistorikkDao,
            dialogDao,
        )

    @Test
    fun `Sett er retur, oppdaterer status på totrinnsvurdering og oppdaterer periodehistorikk med vedtaksperiodeId`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val oppgaveId = 42L

        every { oppgaveDao.finnIdForAktivOppgave(vedtaksperiodeId) } returns oppgaveId
        every { oppgaveDao.finnIdForAktivOppgave(any()) } returns oppgaveId

        totrinnsvurderingService.settAutomatiskRetur(vedtaksperiodeId)

        verify(exactly = 1) { totrinnsvurderingDao.settErRetur(vedtaksperiodeId) }
        verify(exactly = 1) { oppgaveDao.finnIdForAktivOppgave(vedtaksperiodeId) }
        verify(exactly = 1) {
            periodehistorikkDao.lagreMedOppgaveId(
                historikkinnslag = any<TotrinnsvurderingAutomatiskRetur>(),
                oppgaveId = oppgaveId,
            )
        }
    }
}
