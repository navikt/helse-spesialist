package no.nav.helse.spesialist.application.modell

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.MeldingDao
import no.nav.helse.db.ReservasjonDao
import no.nav.helse.db.VedtakDao
import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.VurderVidereBehandlingAvGodkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.spesialist.application.Testdata.godkjenningsbehovData
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.oppgave.Oppgave
import no.nav.helse.spesialist.domain.testfixtures.lagOppgave
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

internal class VurderVidereBehandlingAvGodkjenningsbehovTest {
    private val oppgaveRepository = mockk<OppgaveRepository>(relaxed = true)
    private val reservasjonDao = mockk<ReservasjonDao>(relaxed = true)
    private val vedtakDao = mockk<VedtakDao>(relaxed = true)
    private val meldingDao = mockk<MeldingDao>(relaxed = true)

    @Test
    fun `oppdaterer oppgavens peker til godkjenningsbehov ved mottak av nytt godkjenningsbehov`() {
        val spleisBehandlingId = UUID.randomUUID()
        val oldData = godkjenningsbehovData(spleisBehandlingId = spleisBehandlingId, tags = listOf("GAMMEL_KJEDELIG_TAG"))
        val newData = godkjenningsbehovData(spleisBehandlingId = spleisBehandlingId, tags = listOf("NY_OG_BANEBRYTENDE_TAG"))

        val oppgave = lagOppgave(behandlingId = SpleisBehandlingId(spleisBehandlingId), godkjenningsbehovId = oldData.id)

        val oldGodkjenningsbehov = mockk<Godkjenningsbehov>(relaxed = true)
        every { oldGodkjenningsbehov.data() } returns oldData

        every { vedtakDao.erAutomatiskGodkjent(newData.utbetalingId) } returns false
        every { oppgaveRepository.finn(SpleisBehandlingId(spleisBehandlingId)) } returns oppgave
        every { meldingDao.finn(oldData.id) } returns oldGodkjenningsbehov

        val command =
            VurderVidereBehandlingAvGodkjenningsbehov(
                fødselsnummer = newData.fødselsnummer,
                commandData = newData,
                oppgaveRepository = oppgaveRepository,
                reservasjonDao = reservasjonDao,
                vedtakDao = vedtakDao,
                meldingDao = meldingDao,
            )
        command.execute(CommandContext(UUID.randomUUID()))

        assertEquals(newData.id, oppgave.godkjenningsbehovId)
        assertEquals(Oppgave.Invalidert, oppgave.tilstand)
        verify(exactly = 2) { oppgaveRepository.lagre(oppgave) }
    }
}
