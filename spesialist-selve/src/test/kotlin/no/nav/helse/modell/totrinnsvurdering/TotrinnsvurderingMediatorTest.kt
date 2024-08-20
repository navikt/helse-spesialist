package no.nav.helse.modell.totrinnsvurdering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.spesialist.api.notat.NotatRepository
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import org.junit.jupiter.api.Test
import java.util.UUID

class TotrinnsvurderingMediatorTest {
    private val totrinnsvurderingDao = mockk<TotrinnsvurderingDao>(relaxed = true)
    private val notatRepository = mockk<NotatRepository>(relaxed = true)

    val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    private val periodehistorikkDao = mockk<PeriodehistorikkDao>(relaxed = true)
    private val totrinnsvurderingMediator =
        TotrinnsvurderingMediator(
            totrinnsvurderingDao,
            oppgaveDao,
            periodehistorikkDao,
            notatRepository,
        )

    @Test
    fun `Sett er retur, oppdaterer status på totrinnsvurdering og oppdaterer periodehistorikk med vedtaksperiodeId`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val oppgaveId = 42L
        val utbetalingId = UUID.randomUUID()

        every { oppgaveDao.finnIdForAktivOppgave(vedtaksperiodeId) } returns oppgaveId
        every { oppgaveDao.finnIdForAktivOppgave(any()) } returns oppgaveId
        every { oppgaveDao.finnUtbetalingId(any<Long>()) } returns utbetalingId

        totrinnsvurderingMediator.settAutomatiskRetur(vedtaksperiodeId)

        verify(exactly = 1) { totrinnsvurderingDao.settErRetur(vedtaksperiodeId) }
        verify(exactly = 1) { oppgaveDao.finnIdForAktivOppgave(vedtaksperiodeId) }
        verify(exactly = 1) {
            periodehistorikkDao.lagre(
                historikkType = PeriodehistorikkType.TOTRINNSVURDERING_RETUR,
                saksbehandlerOid = null,
                utbetalingId = utbetalingId,
            )
        }
    }
}
