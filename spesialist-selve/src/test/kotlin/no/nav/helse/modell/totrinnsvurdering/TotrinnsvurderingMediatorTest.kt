package no.nav.helse.modell.totrinnsvurdering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime.now
import java.util.UUID
import no.nav.helse.modell.oppgave.OppgaveMediator
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType
import org.junit.jupiter.api.Test

class TotrinnsvurderingMediatorTest {
    private val totrinnsvurderingDao = mockk<TotrinnsvurderingDao>(relaxed = true)
    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)
    private val notatMediator = mockk<NotatMediator>(relaxed = true)


    private val totrinnsvurderingMediator = TotrinnsvurderingMediator(totrinnsvurderingDao, oppgaveMediator, notatMediator)

    @Test
    fun `Sett er retur, oppdaterer status på totrinnsvurdering og oppdaterer periodehistorikk med oppgaveId`() {
        val oppgaveId = 1L
        val beslutterOid = UUID.randomUUID()
        val notat = "notat"

        every { notatMediator.lagreForOppgaveId(oppgaveId, notat, beslutterOid, NotatType.Retur) } returns 1L

        totrinnsvurderingMediator.settRetur(oppgaveId, beslutterOid, notat)

        verify(exactly = 1) { totrinnsvurderingDao.settErRetur(oppgaveId) }
        verify(exactly = 1) {
            oppgaveMediator.lagrePeriodehistorikk(
                oppgaveId = oppgaveId,
                saksbehandleroid = beslutterOid,
                type = PeriodehistorikkType.TOTRINNSVURDERING_RETUR,
                notatId = 1
            )
        }
    }

    @Test
    fun `Sett er retur, oppdaterer status på totrinnsvurdering og oppdaterer periodehistorikk med vedtaksperiodeId`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val oppgaveId = 42L

        every { oppgaveMediator.finnNyesteOppgaveId(vedtaksperiodeId) } returns oppgaveId

        totrinnsvurderingMediator.settAutomatiskRetur(vedtaksperiodeId)

        verify(exactly = 1) { totrinnsvurderingDao.settErRetur(vedtaksperiodeId) }
        verify(exactly = 1) { oppgaveMediator.finnNyesteOppgaveId(vedtaksperiodeId) }
        verify(exactly = 1) {
            oppgaveMediator.lagrePeriodehistorikk(
                oppgaveId = oppgaveId,
                saksbehandleroid = null,
                type = PeriodehistorikkType.TOTRINNSVURDERING_RETUR,
            )
        }
    }

    @Test
    fun `Oppretter ny totrinnsvurdering dersom den kun eksisterer i gammel løsning med kopi av data fra gammel løsning`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val oppgaveId = 42L

        every { oppgaveMediator.finnTotrinnsvurderingFraLegacy(oppgaveId)} returns Totrinnsvurdering(
            vedtaksperiodeId = vedtaksperiodeId,
            erRetur = false,
            saksbehandler = UUID.randomUUID(),
            beslutter = UUID.randomUUID(),
            utbetalingIdRef = null,
            opprettet = now(),
            oppdatert = now()
        )

        totrinnsvurderingMediator.opprettFraLegacy(oppgaveId)

        verify(exactly = 1) { oppgaveMediator.finnTotrinnsvurderingFraLegacy(oppgaveId) }
        verify(exactly = 1) { oppgaveMediator.settTotrinnsoppgaveFalse(oppgaveId) }

        val totrinnLegacy = requireNotNull(oppgaveMediator.finnTotrinnsvurderingFraLegacy(oppgaveId))

        verify(exactly = 1) { totrinnsvurderingDao.opprettFraLegacy(totrinnLegacy) }
    }
}
