package no.nav.helse.modell.totrinnsvurdering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.db.PgOppgaveDao
import no.nav.helse.modell.periodehistorikk.TotrinnsvurderingAutomatiskRetur
import org.junit.jupiter.api.Test
import java.util.UUID

class TotrinnsvurderingServiceTest {
    private val totrinnsvurderingDao = mockk<TotrinnsvurderingDao>(relaxed = true)

    val pgOppgaveDao = mockk<PgOppgaveDao>(relaxed = true)
    private val periodehistorikkDao = mockk<PeriodehistorikkDao>(relaxed = true)
    private val totrinnsvurderingService =
        TotrinnsvurderingService(
            totrinnsvurderingDao,
            pgOppgaveDao,
            periodehistorikkDao,
        )

    @Test
    fun `Sett er retur, oppdaterer status på totrinnsvurdering og oppdaterer periodehistorikk med vedtaksperiodeId`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val oppgaveId = 42L

        every { pgOppgaveDao.finnIdForAktivOppgave(vedtaksperiodeId) } returns oppgaveId
        every { pgOppgaveDao.finnIdForAktivOppgave(any()) } returns oppgaveId

        totrinnsvurderingService.settAutomatiskRetur(vedtaksperiodeId)

        verify(exactly = 1) { totrinnsvurderingDao.settErRetur(vedtaksperiodeId) }
        verify(exactly = 1) { pgOppgaveDao.finnIdForAktivOppgave(vedtaksperiodeId) }
        verify(exactly = 1) {
            periodehistorikkDao.lagre(
                historikkinnslag = any<TotrinnsvurderingAutomatiskRetur>(),
                oppgaveId = oppgaveId,
            )
        }
    }
}
