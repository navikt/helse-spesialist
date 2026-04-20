package no.nav.helse.spesialist.application.kommando

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.VurderVidereBehandlingAvGodkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.spesialist.application.Testdata
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.oppgave.Oppgave
import no.nav.helse.spesialist.domain.testfixtures.lagOppgave
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class VurderVidereBehandlingAvGodkjenningsbehovTest : ApplicationTest() {
    @Test
    fun `oppdaterer oppgavens peker til godkjenningsbehov ved mottak av nytt godkjenningsbehov`() {
        // given
        val spleisBehandlingId = UUID.randomUUID()
        val oldData =
            Testdata.godkjenningsbehovData(
                spleisBehandlingId = spleisBehandlingId,
                tags = listOf("GAMMEL_KJEDELIG_TAG"),
            )
        val newData =
            Testdata.godkjenningsbehovData(
                spleisBehandlingId = spleisBehandlingId,
                tags = listOf("NY_OG_BANEBRYTENDE_TAG"),
            )

        val oppgave =
            lagOppgave(behandlingId = SpleisBehandlingId(spleisBehandlingId), godkjenningsbehovId = oldData.id)

        val oldGodkjenningsbehov = mockk<Godkjenningsbehov>(relaxed = true)
        every { oldGodkjenningsbehov.data() } returns oldData
        every { oldGodkjenningsbehov.id } returns oldData.id

        sessionContext.oppgaveRepository.lagre(oppgave)
        sessionContext.meldingDao.lagre(oldGodkjenningsbehov)

        val command =
            VurderVidereBehandlingAvGodkjenningsbehov(
                fødselsnummer = newData.fødselsnummer,
                commandData = newData,
            )

        // when
        command.execute(CommandContext(UUID.randomUUID()), sessionContext, outbox)

        // then
        val lagretOppgave = sessionContext.oppgaveRepository.finn(oppgave.id)
        assertNotNull(lagretOppgave)
        assertEquals(newData.id, lagretOppgave.godkjenningsbehovId)
        assertEquals(Oppgave.Invalidert, lagretOppgave.tilstand)
    }
}
