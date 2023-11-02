package no.nav.helse.modell.totrinnsvurdering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.db.TotrinnsvurderingDao
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import org.junit.jupiter.api.Test

class TotrinnsvurderingMediatorTest {
    private val totrinnsvurderingDao = mockk<TotrinnsvurderingDao>(relaxed = true)
    private val notatMediator = mockk<NotatMediator>(relaxed = true)


    val oppgaveDao = mockk<OppgaveDao>(relaxed = true)
    val periodehistorikkDao = mockk<PeriodehistorikkDao>(relaxed = true)
    private val totrinnsvurderingMediator = TotrinnsvurderingMediator(
        totrinnsvurderingDao,
        oppgaveDao,
        periodehistorikkDao,
        notatMediator,
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
                utbetalingId = utbetalingId
            )
        }
    }
}
